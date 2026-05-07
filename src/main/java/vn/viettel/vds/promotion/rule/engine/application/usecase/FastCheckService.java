package vn.viettel.vds.promotion.rule.engine.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.FastCheckRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.FastCheckResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.in.FastCheckUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleConfigurationPort;
import vn.viettel.vds.promotion.rule.engine.domain.model.FastCheckRule;
import vn.viettel.vds.promotion.rule.engine.domain.model.RuleConfiguration;

import java.time.*;

/**
 * Fast check service implementation using simple if-else logic.
 * NO Drools engine invocation - pure Java conditions for speed.
 * <p>
 * These checks are:
 * 1. Deterministic - same input always produces same output
 * 2. Simple - no complex business logic
 * 3. Fast - < 5ms execution time
 * 4. Cacheable - results can be cached for identical inputs
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FastCheckService implements FastCheckUseCase {

    // Redis key patterns
    private static final String BLACKLIST_KEY = "fast:blacklist:%s"; // customerId
    private static final String RATE_LIMIT_KEY = "fast:rate:%s:%s"; // customerId:window
    private static final String HOLIDAY_KEY = "fast:holiday:%s"; // date
    private final RuleConfigurationPort ruleConfigurationPort;
    private final RedissonClient redissonClient;

    @Override
    public FastCheckResponse performFastCheck(FastCheckRequest request) {

        // Load fast check rules for the canonical (subjectType, subjectKey).
        // Fail-closed: a null config means no rule is configured for this
        // subject, which must deny rather than silently allow.
        RuleConfiguration config = ruleConfigurationPort.getConfiguration(
                request.getSubjectType(),
                request.getCampaignId()
        );

        if (config == null) {
            log.warn("Fast-check denied — no rule configured for {}:{}",
                    request.getSubjectType(), request.getCampaignId());
            return FastCheckResponse.deny("NO_RULE_CONFIGURED",
                    "No validation rule configured for " + request.getSubjectType() + ":" + request.getCampaignId());
        }

        if (!config.hasFastCheckRules()) {
            // Config row exists but has no gate constraints — allow through
            // to full Drools evaluation.
            return FastCheckResponse.allow("Config row has no fast-check constraints");
        }

        // 1. TIME CHECKS (fastest - just timestamp comparison)
        if (config.hasTimeConstraints()) {
            TimeCheckResult timeResult = checkTimeConstraints(config, request);
            if (!timeResult.passed) {
                return FastCheckResponse.deny(timeResult.failureCode, timeResult.explanation);
            }
        }

        // 2. ORDER VALUE CHECKS (very fast - number comparison)
        if (config.hasOrderConstraints()) {
            OrderCheckResult orderResult = checkOrderConstraints(config, request);
            if (!orderResult.passed) {
                return FastCheckResponse.deny(orderResult.failureCode, orderResult.explanation);
            }
        }

        // 3. BLACKLIST CHECK (fast - Redis set lookup)
        if (config.hasBlacklistCheck()) {
            BlacklistCheckResult blacklistResult = checkBlacklist(request);
            if (!blacklistResult.passed) {
                return FastCheckResponse.deny(blacklistResult.failureCode, blacklistResult.explanation);
            }
        }

        // 4. RATE LIMIT CHECK (fast - Redis counter)
        if (config.hasRateLimiting()) {
            RateLimitResult rateResult = checkRateLimit(config, request);
            if (!rateResult.passed) {
                return FastCheckResponse.deny(rateResult.failureCode, rateResult.explanation);
            }
        }

        // All fast checks passed
        return FastCheckResponse.allow("All fast checks passed");
    }

    /**
     * Time-based checks - business hours, blackout periods, day of week
     */
    private TimeCheckResult checkTimeConstraints(RuleConfiguration config, FastCheckRequest request) {

        Instant now = request.getEvaluationTime() != null ?
                request.getEvaluationTime().toInstant() : Instant.now();

        String timezone = request.getTimezone() != null ?
                request.getTimezone() : "Asia/Ho_Chi_Minh";

        ZonedDateTime zonedTime = now.atZone(ZoneId.of(timezone));

        // Check blackout periods
        for (FastCheckRule.BlackoutPeriod blackout : config.getBlackoutPeriods()) {
            if (isInBlackoutPeriod(zonedTime, blackout)) {
                return new TimeCheckResult(false, "BLACKOUT_PERIOD",
                        String.format("Currently in blackout period: %s", blackout.getName()));
            }
        }

        // Check business hours
        if (config.getBusinessHours() != null && !isInBusinessHours(zonedTime, config.getBusinessHours())) {
            return new TimeCheckResult(false, "OUTSIDE_BUSINESS_HOURS",
                    String.format("Outside business hours: %s-%s",
                            config.getBusinessHours().getStart(),
                            config.getBusinessHours().getEnd()));
        }

        // Check allowed days of week
        if (!config.getAllowedDaysOfWeek().isEmpty()) {
            String currentDay = zonedTime.getDayOfWeek().name();
            if (!config.getAllowedDaysOfWeek().contains(currentDay)) {
                return new TimeCheckResult(false, "DAY_NOT_ALLOWED",
                        String.format("Current day %s not in allowed days", currentDay));
            }
        }

        // Check if holiday (from Redis)
        if (config.isExcludeHolidays() && isHoliday(zonedTime.toLocalDate())) {
            return new TimeCheckResult(false, "HOLIDAY_EXCLUSION",
                    "Promotions not available on holidays");
        }

        return new TimeCheckResult(true, null, null);
    }

    /**
     * Order value and item checks
     */
    private OrderCheckResult checkOrderConstraints(RuleConfiguration config, FastCheckRequest request) {

        Long orderTotal = request.getOrderTotal();
        if (orderTotal == null) {
            orderTotal = 0L;
        }

        // Minimum order value
        if (config.getMinOrderValue() > 0 && orderTotal < config.getMinOrderValue()) {
            return new OrderCheckResult(false, "ORDER_VALUE_TOO_LOW",
                    String.format("Order value %d below minimum %d", orderTotal, config.getMinOrderValue()));
        }

        // Maximum order value
        if (config.getMaxOrderValue() > 0 && orderTotal > config.getMaxOrderValue()) {
            return new OrderCheckResult(false, "ORDER_VALUE_TOO_HIGH",
                    String.format("Order value %d exceeds maximum %d", orderTotal, config.getMaxOrderValue()));
        }

        // Currency check
        String currency = request.getCurrency() != null ? request.getCurrency() : "VND";
        if (!config.getAllowedCurrencies().contains(currency)) {
            return new OrderCheckResult(false, "CURRENCY_NOT_ALLOWED",
                    String.format("Currency %s not allowed", currency));
        }

        // Item count check
        int itemCount = request.getItemCount() != null ? request.getItemCount() : 0;
        if (config.getMinItems() > 0 && itemCount < config.getMinItems()) {
            return new OrderCheckResult(false, "TOO_FEW_ITEMS",
                    String.format("Item count %d below minimum %d", itemCount, config.getMinItems()));
        }

        if (config.getMaxItems() > 0 && itemCount > config.getMaxItems()) {
            return new OrderCheckResult(false, "TOO_MANY_ITEMS",
                    String.format("Item count %d exceeds maximum %d", itemCount, config.getMaxItems()));
        }

        return new OrderCheckResult(true, null, null);
    }

    /**
     * Blacklist check using Redisson
     */
    private BlacklistCheckResult checkBlacklist(FastCheckRequest request) {
        try {
            // Check customer-specific blacklist using RBucket
            String customerKey = String.format(BLACKLIST_KEY, request.getCustomerId());
            RBucket<Boolean> bucket = redissonClient.getBucket(customerKey);
            Boolean isBlacklisted = bucket.get();

            if (Boolean.TRUE.equals(isBlacklisted)) {
                return new BlacklistCheckResult(false, "CUSTOMER_BLACKLISTED",
                        "Customer is blacklisted");
            }

            // Check global blacklist set using RSet
            String globalKey = "fast:blacklist:global";
            RSet<String> globalBlacklist = redissonClient.getSet(globalKey);
            Boolean isMember = globalBlacklist.contains(request.getCustomerId());

            if (Boolean.TRUE.equals(isMember)) {
                // Cache for faster future checks
                bucket.set(true, java.time.Duration.ofHours(1));
                return new BlacklistCheckResult(false, "CUSTOMER_BLACKLISTED_GLOBAL",
                        "Customer is in global blacklist");
            }

            return new BlacklistCheckResult(true, null, null);

        } catch (Exception e) {
            log.warn("Blacklist check failed: {}", e.getMessage());
            // Fail open - allow if Redisson is down
            return new BlacklistCheckResult(true, null, null);
        }
    }

    /**
     * Rate limiting using Redisson atomic counters
     */
    private RateLimitResult checkRateLimit(RuleConfiguration config, FastCheckRequest request) {
        try {
            String customerId = request.getCustomerId();

            // Check hourly limit using RAtomicLong
            if (config.getMaxPerHour() > 0) {
                String hourKey = String.format(RATE_LIMIT_KEY, customerId, "hour");
                RAtomicLong hourCounter = redissonClient.getAtomicLong(hourKey);
                long count = hourCounter.incrementAndGet();

                if (count == 1) {
                    hourCounter.expire(java.time.Duration.ofHours(1));
                }

                if (count > config.getMaxPerHour()) {
                    return new RateLimitResult(false, "RATE_LIMIT_HOUR",
                            String.format("Exceeded hourly limit of %d", config.getMaxPerHour()));
                }
            }

            // Check daily limit using RAtomicLong
            if (config.getMaxPerDay() > 0) {
                String dayKey = String.format(RATE_LIMIT_KEY, customerId, "day");
                RAtomicLong dayCounter = redissonClient.getAtomicLong(dayKey);
                long count = dayCounter.incrementAndGet();

                if (count == 1) {
                    dayCounter.expire(java.time.Duration.ofHours(24));
                }

                if (count > config.getMaxPerDay()) {
                    return new RateLimitResult(false, "RATE_LIMIT_DAY",
                            String.format("Exceeded daily limit of %d", config.getMaxPerDay()));
                }
            }

            return new RateLimitResult(true, null, null);

        } catch (Exception e) {
            log.warn("Rate limit check failed: {}", e.getMessage());
            // Fail open - allow if Redisson is down
            return new RateLimitResult(true, null, null);
        }
    }

    // Helper methods
    private boolean isInBlackoutPeriod(ZonedDateTime time, FastCheckRule.BlackoutPeriod blackout) {
        ZonedDateTime start = blackout.getStart().atZone(time.getZone());
        ZonedDateTime end = blackout.getEnd().atZone(time.getZone());
        return time.isAfter(start) && time.isBefore(end);
    }

    private boolean isInBusinessHours(ZonedDateTime time, FastCheckRule.BusinessHours hours) {
        LocalTime currentTime = time.toLocalTime();
        return !currentTime.isBefore(hours.getStart()) && !currentTime.isAfter(hours.getEnd());
    }

    private boolean isHoliday(LocalDate date) {
        try {
            String holidayKey = String.format(HOLIDAY_KEY, date.toString());
            RBucket<Boolean> holidayBucket = redissonClient.getBucket(holidayKey);
            return Boolean.TRUE.equals(holidayBucket.get());
        } catch (Exception e) {
            return false;
        }
    }

    // Result records
    private record TimeCheckResult(boolean passed, String failureCode, String explanation) {
    }

    private record OrderCheckResult(boolean passed, String failureCode, String explanation) {
    }

    private record BlacklistCheckResult(boolean passed, String failureCode, String explanation) {
    }

    private record RateLimitResult(boolean passed, String failureCode, String explanation) {
    }
}