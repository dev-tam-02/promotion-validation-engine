package vn.viettel.vds.promotion.validation.engine.domain.model;

import java.util.List;
import java.util.Set;

/**
 * Rule configuration for fast check operations.
 * This is a wrapper/adapter for ValidationRuleConfig to provide fast check specific methods.
 */
public class RuleConfiguration {

    private final ValidationRuleConfig config;

    public RuleConfiguration(ValidationRuleConfig config) {
        this.config = config;
    }

    public boolean hasFastCheckRules() {
        return config != null && config.hasFastCheckRules();
    }

    // Time constraints
    public boolean hasTimeConstraints() {
        return config.getFastCheckRules() != null &&
               (getBlackoutPeriods() != null && !getBlackoutPeriods().isEmpty()
                || getBusinessHours() != null
                || getAllowedDaysOfWeek() != null && !getAllowedDaysOfWeek().isEmpty()
                || isExcludeHolidays());
    }

    public List<FastCheckRule.BlackoutPeriod> getBlackoutPeriods() {
        if (config.getFastCheckRules() == null || config.getFastCheckRules().getBlackoutPeriods() == null) {
            return List.of();
        }
        return config.getFastCheckRules().getBlackoutPeriods().stream()
            .map(period -> new FastCheckRule.BlackoutPeriod(
                period.getName(),
                period.getStartTime(),
                period.getEndTime()
            ))
            .toList();
    }

    public FastCheckRule.BusinessHours getBusinessHours() {
        if (config.getFastCheckRules() == null || config.getFastCheckRules().getBusinessHours() == null) {
            return null;
        }
        ValidationRuleConfig.BusinessHours bh = config.getFastCheckRules().getBusinessHours();
        return new FastCheckRule.BusinessHours(bh.getStartTime(), bh.getEndTime());
    }

    public Set<String> getAllowedDaysOfWeek() {
        if (config.getFastCheckRules() == null) {
            return Set.of();
        }
        return config.getFastCheckRules().getAllowedDaysOfWeek() != null
            ? config.getFastCheckRules().getAllowedDaysOfWeek()
            : Set.of();
    }

    public boolean isExcludeHolidays() {
        return config.getFastCheckRules() != null && config.getFastCheckRules().isExcludeHolidays();
    }

    // Order constraints
    public boolean hasOrderConstraints() {
        return config.getFastCheckRules() != null &&
               (getMinOrderValue() > 0 || getMaxOrderValue() > 0
                || getAllowedCurrencies() != null && !getAllowedCurrencies().isEmpty()
                || getMinItems() > 0 || getMaxItems() > 0);
    }

    public long getMinOrderValue() {
        if (config.getFastCheckRules() == null || config.getFastCheckRules().getMinOrderValue() == null) {
            return 0;
        }
        return config.getFastCheckRules().getMinOrderValue();
    }

    public long getMaxOrderValue() {
        if (config.getFastCheckRules() == null || config.getFastCheckRules().getMaxOrderValue() == null) {
            return 0;
        }
        return config.getFastCheckRules().getMaxOrderValue();
    }

    public Set<String> getAllowedCurrencies() {
        if (config.getFastCheckRules() == null) {
            return Set.of("VND"); // Default currency
        }
        return config.getFastCheckRules().getAllowedCurrencies() != null
            ? config.getFastCheckRules().getAllowedCurrencies()
            : Set.of("VND");
    }

    public int getMinItems() {
        if (config.getFastCheckRules() == null || config.getFastCheckRules().getMinItems() == null) {
            return 0;
        }
        return config.getFastCheckRules().getMinItems();
    }

    public int getMaxItems() {
        if (config.getFastCheckRules() == null || config.getFastCheckRules().getMaxItems() == null) {
            return 0;
        }
        return config.getFastCheckRules().getMaxItems();
    }

    // Blacklist and rate limiting
    public boolean hasBlacklistCheck() {
        // Always enable blacklist check for security
        return true;
    }

    public boolean hasRateLimiting() {
        return config.getFastCheckRules() != null &&
               (getMaxPerHour() > 0 || getMaxPerDay() > 0);
    }

    public int getMaxPerHour() {
        if (config.getFastCheckRules() == null || config.getFastCheckRules().getMaxPerHour() == null) {
            return 0;
        }
        return config.getFastCheckRules().getMaxPerHour();
    }

    public int getMaxPerDay() {
        if (config.getFastCheckRules() == null || config.getFastCheckRules().getMaxPerDay() == null) {
            return 0;
        }
        return config.getFastCheckRules().getMaxPerDay();
    }

    // Getters for the underlying config
    public ValidationRuleConfig getConfig() {
        return config;
    }
}