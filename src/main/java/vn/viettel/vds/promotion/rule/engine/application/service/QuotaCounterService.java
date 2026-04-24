package vn.viettel.vds.promotion.rule.engine.application.service;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.QuotaEventEntity;
import vn.viettel.vds.promotion.rule.engine.application.port.out.QuotaEventPort;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Layer-1 quota counter service using Redis (Redisson) as a hot counter store.
 *
 * <p>Key format: {@code rule:cnt:{ruleId}:{bucketKey}:{windowStartEpochSec}}
 * <p>TTL is set to (windowEnd - windowStart) seconds so counters auto-expire once the
 * window closes.
 *
 * <p>Every mutation (INCR / DECR) is also journaled to {@code quota_events} in MariaDB
 * so that counters can be fully reconstructed after a Redis outage.
 */
@Service
public class QuotaCounterService {

    private static final Logger log = LoggerFactory.getLogger(QuotaCounterService.class);
    private static final String KEY_PREFIX = "rule:cnt:";

    private final RedissonClient redissonClient;
    private final QuotaEventPort quotaEventPort;

    public QuotaCounterService(RedissonClient redissonClient, QuotaEventPort quotaEventPort) {
        this.redissonClient = redissonClient;
        this.quotaEventPort = quotaEventPort;
    }

    /**
     * Attempt to increment the counter for a rule window.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>INCR the Redis atomic counter.</li>
     *   <li>If the new value exceeds {@code limit} → DECR to undo and return {@code false}.</li>
     *   <li>Otherwise persist an INCR quota_event and return {@code true}.</li>
     * </ol>
     *
     * @return {@code true} if the increment was accepted (new value ≤ limit),
     *         {@code false} if the limit was already reached.
     */
    public boolean incrementWithCheck(String ruleId,
                                      String redemptionId,
                                      String customerId,
                                      String bucketKey,
                                      LocalDateTime windowStart,
                                      LocalDateTime windowEnd,
                                      int limit) {
        String redisKey = buildKey(ruleId, bucketKey, windowStart);
        RAtomicLong counter = redissonClient.getAtomicLong(redisKey);

        long current = counter.incrementAndGet();
        log.debug("QuotaCounter INCR key={} current={} limit={}", redisKey, current, limit);

        if (current > limit) {
            // Over limit — roll back the INCR immediately
            counter.decrementAndGet();
            log.info("QuotaCounter DENY ruleId={} redemptionId={} limit={}", ruleId, redemptionId, limit);
            return false;
        }

        // Set TTL on first write so counter auto-expires at window close
        if (current == 1 && windowStart != null && windowEnd != null) {
            long ttlSeconds = windowEnd.toEpochSecond(ZoneOffset.UTC)
                    - windowStart.toEpochSecond(ZoneOffset.UTC);
            if (ttlSeconds > 0) {
                counter.expire(ttlSeconds, TimeUnit.SECONDS);
            }
        }

        // Journal the INCR to durable storage
        persistEvent(ruleId, redemptionId, customerId, bucketKey, windowStart, windowEnd,
                QuotaEventEntity.EventType.INCR, 1);

        log.info("QuotaCounter ALLOW ruleId={} redemptionId={} current={}", ruleId, redemptionId, current);
        return true;
    }

    /**
     * Decrement the counter for a rule window (used during rollback).
     *
     * <p>Clamps the value at 0 to prevent negative counters from stale DECR calls.
     */
    public void decrement(String ruleId,
                          String redemptionId,
                          String customerId,
                          String bucketKey,
                          LocalDateTime windowStart,
                          LocalDateTime windowEnd) {
        String redisKey = buildKey(ruleId, bucketKey, windowStart);
        RAtomicLong counter = redissonClient.getAtomicLong(redisKey);
        long newValue = counter.decrementAndGet();
        if (newValue < 0) {
            counter.set(0);
        }
        log.debug("QuotaCounter DECR key={} newValue={}", redisKey, newValue);

        persistEvent(ruleId, redemptionId, customerId, bucketKey, windowStart, windowEnd,
                QuotaEventEntity.EventType.ROLLBACK, -1);
    }

    /**
     * Get the current counter value without mutating it (peek mode).
     */
    public long peek(String ruleId, String bucketKey, LocalDateTime windowStart) {
        return redissonClient.getAtomicLong(buildKey(ruleId, bucketKey, windowStart)).get();
    }

    /**
     * Overwrite (SET) a counter to a specific value — used by the rebuild job.
     * TTL is set based on (windowEnd - windowStart) seconds.
     */
    public void overwrite(String ruleId,
                          String bucketKey,
                          LocalDateTime windowStart,
                          LocalDateTime windowEnd,
                          long value) {
        String redisKey = buildKey(ruleId, bucketKey, windowStart);
        RAtomicLong counter = redissonClient.getAtomicLong(redisKey);
        counter.set(value);

        if (windowStart != null && windowEnd != null) {
            long ttlSeconds = windowEnd.toEpochSecond(ZoneOffset.UTC)
                    - windowStart.toEpochSecond(ZoneOffset.UTC);
            if (ttlSeconds > 0) {
                counter.expire(ttlSeconds, TimeUnit.SECONDS);
            }
        }
        log.debug("QuotaCounter OVERWRITE key={} value={}", redisKey, value);
    }

    /**
     * Compute drift between Redis counter and DB aggregate for all active windows.
     *
     * @return average absolute drift ratio (0.0 = no drift, 1.0 = 100% drift)
     */
    public double computeDriftRatio(LocalDateTime threshold) {
        Map<String, Long> dbAggregates = quotaEventPort.aggregateActiveWindowCounters(threshold);
        if (dbAggregates.isEmpty()) {
            return 0.0;
        }

        long totalDrift = 0;
        long totalDb = 0;

        for (Map.Entry<String, Long> entry : dbAggregates.entrySet()) {
            // key format: "{ruleId}:{bucketKey}:{epochSec}:wend:{windowEndEpoch}"
            String[] parts = entry.getKey().split(":");
            if (parts.length < 5) continue;

            String ruleId     = parts[0];
            String bucketKey  = parts[1];
            long windowEpoch  = Long.parseLong(parts[2]);

            LocalDateTime windowStart = LocalDateTime.ofEpochSecond(windowEpoch, 0, ZoneOffset.UTC);
            String redisKey   = buildKey(ruleId, bucketKey, windowStart);
            long redisValue   = redissonClient.getAtomicLong(redisKey).get();
            long dbValue      = entry.getValue();

            totalDrift += Math.abs(redisValue - dbValue);
            totalDb    += Math.abs(dbValue);
        }

        return totalDb == 0 ? 0.0 : (double) totalDrift / totalDb;
    }

    // -------- private helpers --------

    private String buildKey(String ruleId, String bucketKey, LocalDateTime windowStart) {
        long epoch = (windowStart != null) ? windowStart.toEpochSecond(ZoneOffset.UTC) : 0L;
        return KEY_PREFIX + ruleId + ":" + (bucketKey != null ? bucketKey : "") + ":" + epoch;
    }

    private void persistEvent(String ruleId,
                              String redemptionId,
                              String customerId,
                              String bucketKey,
                              LocalDateTime windowStart,
                              LocalDateTime windowEnd,
                              QuotaEventEntity.EventType type,
                              int delta) {
        QuotaEventEntity event = new QuotaEventEntity();
        event.setRuleId(ruleId);
        event.setRedemptionId(redemptionId);
        event.setCustomerId(customerId);
        event.setBucketKey(bucketKey);
        event.setWindowStart(windowStart);
        event.setWindowEnd(windowEnd);
        event.setEventType(type);
        event.setCountDelta(delta);
        event.setCreatedAt(LocalDateTime.now());
        quotaEventPort.save(event);
    }
}
