package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Domain model for validation rule configuration.
 * This represents the complete rule set for a campaign/promotion,
 * which will be split into fast-check and Drools-based rules.
 */
public class ValidationRuleConfig {

    private String ruleId;
    private String campaignId;
    private boolean enabled;

    // Fast-checkable rules (simple if-else)
    private FastCheckRules fastCheckRules;

    // Complex rules (require Drools)
    private String droolsRuleContent;
    private String droolsRuleVersion;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Check if this config has fast-checkable rules
     */
    public boolean hasFastCheckRules() {
        return fastCheckRules != null && fastCheckRules.hasFastCheckRules();
    }

    /**
     * Check if this config has Drools rules
     */
    public boolean hasDroolsRules() {
        return droolsRuleContent != null && !droolsRuleContent.isEmpty();
    }

    // Main getters and setters
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public FastCheckRules getFastCheckRules() {
        return fastCheckRules;
    }

    public void setFastCheckRules(FastCheckRules fastCheckRules) {
        this.fastCheckRules = fastCheckRules;
    }

    public String getDroolsRuleContent() {
        return droolsRuleContent;
    }

    public void setDroolsRuleContent(String droolsRuleContent) {
        this.droolsRuleContent = droolsRuleContent;
    }

    public String getDroolsRuleVersion() {
        return droolsRuleVersion;
    }

    public void setDroolsRuleVersion(String droolsRuleVersion) {
        this.droolsRuleVersion = droolsRuleVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Fast-checkable rules that can be evaluated without Drools
     */
    public static class FastCheckRules {

        // Time constraints - FAST (timestamp comparison)
        private List<BlackoutPeriod> blackoutPeriods;
        private BusinessHours businessHours;
        private Set<String> allowedDaysOfWeek;
        private boolean excludeHolidays;

        // Order constraints - FAST (number comparison)
        private Long minOrderValue;
        private Long maxOrderValue;
        private Set<String> allowedCurrencies;
        private Integer minItems;
        private Integer maxItems;

        // Customer constraints - FAST (set lookup)
        private Set<String> allowedSegments;
        private Set<String> excludedSegments;
        private Set<String> allowedTiers;
        private Set<String> allowedChannels;
        private Set<String> allowedLocations;

        // Rate limits - FAST (Redis counter)
        private Integer maxPerHour;
        private Integer maxPerDay;
        private Integer maxPerCustomer;

        public boolean hasFastCheckRules() {
            return blackoutPeriods != null && !blackoutPeriods.isEmpty()
                    || businessHours != null
                    || allowedDaysOfWeek != null && !allowedDaysOfWeek.isEmpty()
                    || minOrderValue != null || maxOrderValue != null
                    || allowedSegments != null && !allowedSegments.isEmpty()
                    || maxPerHour != null || maxPerDay != null;
        }

        // Getters and setters
        public List<BlackoutPeriod> getBlackoutPeriods() {
            return blackoutPeriods;
        }

        public void setBlackoutPeriods(List<BlackoutPeriod> blackoutPeriods) {
            this.blackoutPeriods = blackoutPeriods;
        }

        public BusinessHours getBusinessHours() {
            return businessHours;
        }

        public void setBusinessHours(BusinessHours businessHours) {
            this.businessHours = businessHours;
        }

        public Set<String> getAllowedDaysOfWeek() {
            return allowedDaysOfWeek;
        }

        public void setAllowedDaysOfWeek(Set<String> allowedDaysOfWeek) {
            this.allowedDaysOfWeek = allowedDaysOfWeek;
        }

        public boolean isExcludeHolidays() {
            return excludeHolidays;
        }

        public void setExcludeHolidays(boolean excludeHolidays) {
            this.excludeHolidays = excludeHolidays;
        }

        public Long getMinOrderValue() {
            return minOrderValue;
        }

        public void setMinOrderValue(Long minOrderValue) {
            this.minOrderValue = minOrderValue;
        }

        public Long getMaxOrderValue() {
            return maxOrderValue;
        }

        public void setMaxOrderValue(Long maxOrderValue) {
            this.maxOrderValue = maxOrderValue;
        }

        public Set<String> getAllowedCurrencies() {
            return allowedCurrencies;
        }

        public void setAllowedCurrencies(Set<String> allowedCurrencies) {
            this.allowedCurrencies = allowedCurrencies;
        }

        public Integer getMinItems() {
            return minItems;
        }

        public void setMinItems(Integer minItems) {
            this.minItems = minItems;
        }

        public Integer getMaxItems() {
            return maxItems;
        }

        public void setMaxItems(Integer maxItems) {
            this.maxItems = maxItems;
        }

        public Set<String> getAllowedSegments() {
            return allowedSegments;
        }

        public void setAllowedSegments(Set<String> allowedSegments) {
            this.allowedSegments = allowedSegments;
        }

        public Set<String> getExcludedSegments() {
            return excludedSegments;
        }

        public void setExcludedSegments(Set<String> excludedSegments) {
            this.excludedSegments = excludedSegments;
        }

        public Set<String> getAllowedTiers() {
            return allowedTiers;
        }

        public void setAllowedTiers(Set<String> allowedTiers) {
            this.allowedTiers = allowedTiers;
        }

        public Set<String> getAllowedChannels() {
            return allowedChannels;
        }

        public void setAllowedChannels(Set<String> allowedChannels) {
            this.allowedChannels = allowedChannels;
        }

        public Set<String> getAllowedLocations() {
            return allowedLocations;
        }

        public void setAllowedLocations(Set<String> allowedLocations) {
            this.allowedLocations = allowedLocations;
        }

        public Integer getMaxPerHour() {
            return maxPerHour;
        }

        public void setMaxPerHour(Integer maxPerHour) {
            this.maxPerHour = maxPerHour;
        }

        public Integer getMaxPerDay() {
            return maxPerDay;
        }

        public void setMaxPerDay(Integer maxPerDay) {
            this.maxPerDay = maxPerDay;
        }

        public Integer getMaxPerCustomer() {
            return maxPerCustomer;
        }

        public void setMaxPerCustomer(Integer maxPerCustomer) {
            this.maxPerCustomer = maxPerCustomer;
        }
    }

    public static class BlackoutPeriod {
        private String name;
        private Instant startTime;
        private Instant endTime;
        private String reason;

        public BlackoutPeriod() {
        }

        public BlackoutPeriod(String name, Instant startTime, Instant endTime) {
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public void setStartTime(Instant startTime) {
            this.startTime = startTime;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class BusinessHours {
        private LocalTime startTime;
        private LocalTime endTime;
        private String timezone;

        public BusinessHours() {
        }

        public BusinessHours(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.timezone = "Asia/Ho_Chi_Minh";
        }

        // Getters and setters
        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }
    }
}