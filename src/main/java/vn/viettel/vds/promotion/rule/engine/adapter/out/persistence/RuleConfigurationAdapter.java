package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.FastCheckConfigEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.FastCheckConfigJpaRepository;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleConfigurationPort;
import vn.viettel.vds.promotion.rule.engine.domain.model.RuleConfiguration;
import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationRuleConfig;
import vn.viettel.vds.promotion.validation.event.FastCheckConfigDto;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
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
            new TypeReference<>() {
            };

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

    /**
     * Upsert a fast-check config row from a typed event payload.
     * <p>
     * Uses {@code sourceVersion} as an optimistic-concurrency guard:
     * if the stored version is greater than or equal to the incoming one,
     * the update is skipped to protect against out-of-order kafka delivery.
     * Invalidates the Caffeine cache on write so subsequent lookups see
     * the new row.
     */
    @Transactional
    public void upsert(String subjectType, String subjectKey,
                       FastCheckConfigDto dto, Long sourceVersion) {
        if (subjectType == null || subjectKey == null || dto == null) {
            log.warn("Refusing fast-check upsert with null key or payload: {}:{}",
                    subjectType, subjectKey);
            return;
        }

        Optional<FastCheckConfigEntity> existing =
                repository.findBySubjectTypeAndSubjectKey(subjectType, subjectKey);

        FastCheckConfigEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            if (sourceVersion != null && entity.getSourceVersion() != null
                    && sourceVersion <= entity.getSourceVersion()) {
                log.debug("Skipping stale fast-check update {}:{} version={} <= {}",
                        subjectType, subjectKey, sourceVersion, entity.getSourceVersion());
                return;
            }
        } else {
            entity = new FastCheckConfigEntity();
            entity.setSubjectType(subjectType);
            entity.setSubjectKey(subjectKey);
        }

        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : Boolean.TRUE);
        entity.setBusinessHoursStart(parseTime(dto.getBusinessHoursStart()));
        entity.setBusinessHoursEnd(parseTime(dto.getBusinessHoursEnd()));
        entity.setBusinessHoursTimezone(dto.getBusinessHoursTimezone());
        entity.setAllowedDaysOfWeekJson(toJson(dto.getAllowedDaysOfWeek()));
        entity.setExcludeHolidays(dto.getExcludeHolidays());
        entity.setMinOrderValue(dto.getMinOrderValue());
        entity.setMaxOrderValue(dto.getMaxOrderValue());
        entity.setMinItems(dto.getMinItems());
        entity.setMaxItems(dto.getMaxItems());
        entity.setAllowedCurrenciesJson(toJson(dto.getAllowedCurrencies()));
        entity.setRequiredSegmentsJson(toJson(dto.getRequiredSegments()));
        entity.setExcludedSegmentsJson(toJson(dto.getExcludedSegments()));
        entity.setRequireAllSegments(dto.getRequireAllSegments());
        entity.setMaxOrderCount(dto.getMaxOrderCount());
        entity.setMaxPerHour(dto.getMaxPerHour());
        entity.setMaxPerDay(dto.getMaxPerDay());
        entity.setMaxPerWeek(dto.getMaxPerWeek());
        entity.setMaxPerMonth(dto.getMaxPerMonth());
        entity.setWindowType(dto.getWindowType());
        entity.setSourceVersion(sourceVersion);
        entity.touchCreated();

        repository.save(entity);
        invalidate(subjectType, subjectKey);
        log.info("Upserted fast-check config {}:{} version={}", subjectType, subjectKey, sourceVersion);
    }

    /**
     * Delete a fast-check row when the upstream rule is deleted.
     */
    @Transactional
    public void delete(String subjectType, String subjectKey) {
        if (subjectType == null || subjectKey == null) {
            return;
        }
        repository.deleteBySubjectTypeAndSubjectKey(subjectType, subjectKey);
        invalidate(subjectType, subjectKey);
        log.info("Deleted fast-check config {}:{}", subjectType, subjectKey);
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (Exception e) {
            log.warn("Unparseable business-hour value '{}'", value);
            return null;
        }
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("Failed to serialize list to JSON: {}", list, e);
            return null;
        }
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
