package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.FastCheckConfigEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.FastCheckConfigJpaRepository;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleConfigurationPort;
import vn.viettel.vds.promotion.rule.engine.domain.model.RuleConfiguration;
import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationRuleConfig;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Loads fast-check configuration from {@code fast_check_configs} keyed on
 * the canonical {@code (subjectType, subjectKey)} tuple. Fails closed: when
 * no row is configured, {@link #getConfiguration(String, String)} returns
 * {@code null} and callers ({@code FastCheckService}) must deny the request
 * with {@code NO_RULE_CONFIGURED} rather than silently allowing it.
 * <p>
 * An in-process Caffeine cache absorbs the common case where the same
 * subject is evaluated many times per second. Entries are invalidated by
 * the kafka bundle-cache-invalidation listener when the publish/update
 * pipeline mutates a config row.
 */
@Component
@Slf4j
public class RuleConfigurationAdapter implements RuleConfigurationPort {

    private static final TypeReference<List<String>> LIST_STRING =
            new TypeReference<>() {};

    private final FastCheckConfigJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final Cache<String, RuleConfiguration> cache;

    public RuleConfigurationAdapter(
            FastCheckConfigJpaRepository repository,
            ObjectMapper objectMapper,
            @Value("${rule-engine.fast-check-config.cache.ttl:PT5M}") Duration cacheTtl,
            @Value("${rule-engine.fast-check-config.cache.max-size:10000}") int cacheMaxSize) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.cache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheTtl)
                .recordStats()
                .build();
        log.info("RuleConfigurationAdapter initialised with ttl={}, maxSize={}",
                cacheTtl, cacheMaxSize);
    }

    @Override
    public RuleConfiguration getConfiguration(String subjectType, String subjectKey) {
        if (subjectType == null || subjectKey == null) {
            log.warn("Refusing lookup with null key: subjectType={}, subjectKey={}",
                    subjectType, subjectKey);
            return null;
        }

        String cacheKey = subjectType + ":" + subjectKey;
        RuleConfiguration cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        return repository.findBySubjectTypeAndSubjectKey(subjectType, subjectKey)
                .map(this::toDomain)
                .map(config -> {
                    cache.put(cacheKey, config);
                    return config;
                })
                .orElseGet(() -> {
                    log.warn("No fast-check config for {}:{} — caller must fail-closed",
                            subjectType, subjectKey);
                    return null;
                });
    }

    /**
     * Invalidate a cache entry when the publish pipeline overwrites or
     * deletes its row. Called by the kafka consumer.
     */
    public void invalidate(String subjectType, String subjectKey) {
        if (subjectType != null && subjectKey != null) {
            cache.invalidate(subjectType + ":" + subjectKey);
        }
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    private RuleConfiguration toDomain(FastCheckConfigEntity entity) {
        ValidationRuleConfig config = new ValidationRuleConfig();
        config.setRuleId(entity.getSubjectType() + ":" + entity.getSubjectKey());
        config.setCampaignId(entity.getSubjectKey());
        config.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        config.setCreatedAt(entity.getCreatedAt());
        config.setUpdatedAt(entity.getUpdatedAt());

        ValidationRuleConfig.FastCheckRules rules = new ValidationRuleConfig.FastCheckRules();

        if (entity.getBusinessHoursStart() != null && entity.getBusinessHoursEnd() != null) {
            ValidationRuleConfig.BusinessHours hours = new ValidationRuleConfig.BusinessHours();
            hours.setStartTime(entity.getBusinessHoursStart());
            hours.setEndTime(entity.getBusinessHoursEnd());
            hours.setTimezone(entity.getBusinessHoursTimezone() != null
                    ? entity.getBusinessHoursTimezone() : "Asia/Ho_Chi_Minh");
            rules.setBusinessHours(hours);
        }

        rules.setAllowedDaysOfWeek(toSet(entity.getAllowedDaysOfWeekJson()));
        rules.setExcludeHolidays(Boolean.TRUE.equals(entity.getExcludeHolidays()));
        rules.setMinOrderValue(entity.getMinOrderValue());
        rules.setMaxOrderValue(entity.getMaxOrderValue());
        rules.setAllowedCurrencies(toSet(entity.getAllowedCurrenciesJson()));
        rules.setMinItems(entity.getMinItems());
        rules.setMaxItems(entity.getMaxItems());
        rules.setAllowedSegments(toSet(entity.getRequiredSegmentsJson()));
        rules.setExcludedSegments(toSet(entity.getExcludedSegmentsJson()));
        rules.setMaxPerHour(entity.getMaxPerHour());
        rules.setMaxPerDay(entity.getMaxPerDay());
        rules.setMaxPerCustomer(entity.getMaxOrderCount());

        config.setFastCheckRules(rules);
        return new RuleConfiguration(config);
    }

    private Set<String> toSet(String json) {
        if (json == null || json.isBlank()) {
            return Set.of();
        }
        try {
            return Set.copyOf(objectMapper.readValue(json, LIST_STRING));
        } catch (Exception e) {
            log.warn("Failed to parse JSON list column: {}", json, e);
            return Set.of();
        }
    }
}
