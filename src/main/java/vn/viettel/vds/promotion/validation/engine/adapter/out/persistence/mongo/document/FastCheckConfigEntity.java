package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * MongoDB entity for storing fast check configurations.
 * These are simple rules that can be evaluated without Drools engine.
 */
@Document(collection = "fast_check_configs")
public class FastCheckConfigEntity {

    @Id
    private String id;

    @Field("campaign_id")
    @Indexed(unique = true)
    private String campaignId;

    @Field("tenant_id")
    @Indexed
    private String tenantId;

    @Field("enabled")
    private boolean enabled = true;

    // Time-based constraints
    @Field("time_constraints")
    private TimeConstraints timeConstraints;

    // Order value constraints
    @Field("order_constraints")
    private OrderConstraints orderConstraints;

    // Customer constraints
    @Field("customer_constraints")
    private CustomerConstraints customerConstraints;

    // Rate limiting configuration
    @Field("rate_limits")
    private RateLimitConfig rateLimits;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Main entity getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TimeConstraints getTimeConstraints() {
        return timeConstraints;
    }

    public void setTimeConstraints(TimeConstraints timeConstraints) {
        this.timeConstraints = timeConstraints;
    }

    public OrderConstraints getOrderConstraints() {
        return orderConstraints;
    }

    public void setOrderConstraints(OrderConstraints orderConstraints) {
        this.orderConstraints = orderConstraints;
    }

    public CustomerConstraints getCustomerConstraints() {
        return customerConstraints;
    }

    public void setCustomerConstraints(CustomerConstraints customerConstraints) {
        this.customerConstraints = customerConstraints;
    }

    public RateLimitConfig getRateLimits() {
        return rateLimits;
    }

    public void setRateLimits(RateLimitConfig rateLimits) {
        this.rateLimits = rateLimits;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Inner classes for nested structures
    public static class TimeConstraints {
        @Field("blackout_periods")
        private List<BlackoutPeriod> blackoutPeriods;

        @Field("business_hours")
        private BusinessHours businessHours;

        @Field("allowed_days_of_week")
        private List<String> allowedDaysOfWeek;

        @Field("exclude_holidays")
        private boolean excludeHolidays;

        @Field("flash_sale_windows")
        private List<FlashSaleWindow> flashSaleWindows;

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

        public List<String> getAllowedDaysOfWeek() {
            return allowedDaysOfWeek;
        }

        public void setAllowedDaysOfWeek(List<String> allowedDaysOfWeek) {
            this.allowedDaysOfWeek = allowedDaysOfWeek;
        }

        public boolean isExcludeHolidays() {
            return excludeHolidays;
        }

        public void setExcludeHolidays(boolean excludeHolidays) {
            this.excludeHolidays = excludeHolidays;
        }

        public List<FlashSaleWindow> getFlashSaleWindows() {
            return flashSaleWindows;
        }

        public void setFlashSaleWindows(List<FlashSaleWindow> flashSaleWindows) {
            this.flashSaleWindows = flashSaleWindows;
        }

        public static class BlackoutPeriod {
            private String name;
            private LocalDateTime start;
            private LocalDateTime end;
            private String reason;

            // Getters and setters
            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public LocalDateTime getStart() {
                return start;
            }

            public void setStart(LocalDateTime start) {
                this.start = start;
            }

            public LocalDateTime getEnd() {
                return end;
            }

            public void setEnd(LocalDateTime end) {
                this.end = end;
            }

            public String getReason() {
                return reason;
            }

            public void setReason(String reason) {
                this.reason = reason;
            }
        }

        public static class BusinessHours {
            private LocalTime start;
            private LocalTime end;
            private String timezone;

            // Getters and setters
            public LocalTime getStart() {
                return start;
            }

            public void setStart(LocalTime start) {
                this.start = start;
            }

            public LocalTime getEnd() {
                return end;
            }

            public void setEnd(LocalTime end) {
                this.end = end;
            }

            public String getTimezone() {
                return timezone;
            }

            public void setTimezone(String timezone) {
                this.timezone = timezone;
            }
        }

        public static class FlashSaleWindow {
            private String name;
            private LocalTime dailyStart;
            private LocalTime dailyEnd;
            private List<String> activeDays;

            // Getters and setters
            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public LocalTime getDailyStart() {
                return dailyStart;
            }

            public void setDailyStart(LocalTime dailyStart) {
                this.dailyStart = dailyStart;
            }

            public LocalTime getDailyEnd() {
                return dailyEnd;
            }

            public void setDailyEnd(LocalTime dailyEnd) {
                this.dailyEnd = dailyEnd;
            }

            public List<String> getActiveDays() {
                return activeDays;
            }

            public void setActiveDays(List<String> activeDays) {
                this.activeDays = activeDays;
            }
        }
    }

    public static class OrderConstraints {
        @Field("min_order_value")
        private Long minOrderValue;

        @Field("max_order_value")
        private Long maxOrderValue;

        @Field("allowed_currencies")
        private List<String> allowedCurrencies;

        @Field("min_items")
        private Integer minItems;

        @Field("max_items")
        private Integer maxItems;

        @Field("required_categories")
        private List<String> requiredCategories;

        @Field("excluded_categories")
        private List<String> excludedCategories;

        // Getters and setters
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

        public List<String> getAllowedCurrencies() {
            return allowedCurrencies;
        }

        public void setAllowedCurrencies(List<String> allowedCurrencies) {
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

        public List<String> getRequiredCategories() {
            return requiredCategories;
        }

        public void setRequiredCategories(List<String> requiredCategories) {
            this.requiredCategories = requiredCategories;
        }

        public List<String> getExcludedCategories() {
            return excludedCategories;
        }

        public void setExcludedCategories(List<String> excludedCategories) {
            this.excludedCategories = excludedCategories;
        }
    }

    public static class CustomerConstraints {
        @Field("allowed_segments")
        private List<String> allowedSegments;

        @Field("excluded_segments")
        private List<String> excludedSegments;

        @Field("allowed_regions")
        private List<String> allowedRegions;

        @Field("excluded_regions")
        private List<String> excludedRegions;

        @Field("require_all_segments")
        private boolean requireAllSegments;

        @Field("max_order_count")
        private Integer maxOrderCount;

        // Getters and setters
        public List<String> getAllowedSegments() {
            return allowedSegments;
        }

        public void setAllowedSegments(List<String> allowedSegments) {
            this.allowedSegments = allowedSegments;
        }

        public List<String> getExcludedSegments() {
            return excludedSegments;
        }

        public void setExcludedSegments(List<String> excludedSegments) {
            this.excludedSegments = excludedSegments;
        }

        public List<String> getAllowedRegions() {
            return allowedRegions;
        }

        public void setAllowedRegions(List<String> allowedRegions) {
            this.allowedRegions = allowedRegions;
        }

        public List<String> getExcludedRegions() {
            return excludedRegions;
        }

        public void setExcludedRegions(List<String> excludedRegions) {
            this.excludedRegions = excludedRegions;
        }

        public boolean isRequireAllSegments() {
            return requireAllSegments;
        }

        public void setRequireAllSegments(boolean requireAllSegments) {
            this.requireAllSegments = requireAllSegments;
        }

        public Integer getMaxOrderCount() {
            return maxOrderCount;
        }

        public void setMaxOrderCount(Integer maxOrderCount) {
            this.maxOrderCount = maxOrderCount;
        }
    }

    public static class RateLimitConfig {
        @Field("max_per_hour")
        private Integer maxPerHour;

        @Field("max_per_day")
        private Integer maxPerDay;

        @Field("max_per_week")
        private Integer maxPerWeek;

        @Field("max_per_month")
        private Integer maxPerMonth;

        @Field("window_type")
        private String windowType; // SLIDING or FIXED

        // Getters and setters
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

        public Integer getMaxPerWeek() {
            return maxPerWeek;
        }

        public void setMaxPerWeek(Integer maxPerWeek) {
            this.maxPerWeek = maxPerWeek;
        }

        public Integer getMaxPerMonth() {
            return maxPerMonth;
        }

        public void setMaxPerMonth(Integer maxPerMonth) {
            this.maxPerMonth = maxPerMonth;
        }

        public String getWindowType() {
            return windowType;
        }

        public void setWindowType(String windowType) {
            this.windowType = windowType;
        }
    }
}