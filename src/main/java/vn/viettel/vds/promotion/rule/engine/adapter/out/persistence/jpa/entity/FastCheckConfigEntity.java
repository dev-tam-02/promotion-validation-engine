package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for storing fast check configurations.
 * These are simple rules that can be evaluated without Drools engine.
 */
@Entity
@Table(name = "fast_check_configs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campaign_id", columnNames = {"campaign_id"})
        },
        indexes = {
                @Index(name = "idx_tenant_id", columnList = "tenant_id")
        }
)
@Getter
@Setter
public class FastCheckConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false, unique = true, length = 100)
    private String campaignId;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Embedded
    private TimeConstraints timeConstraints;

    @Embedded
    private OrderConstraints orderConstraints;

    @Embedded
    private CustomerConstraints customerConstraints;

    @Embedded
    private RateLimitConfig rateLimits;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;

    // Embeddable classes for nested structures
    @Embeddable
    @Getter
    @Setter
    public static class TimeConstraints {

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "fast_check_config_id")
        private List<BlackoutPeriod> blackoutPeriods = new ArrayList<>();

        @Embedded
        @AttributeOverride(name = "start", column = @Column(name = "business_hours_start"))
        @AttributeOverride(name = "end", column = @Column(name = "business_hours_end"))
        @AttributeOverride(name = "timezone", column = @Column(name = "business_hours_timezone"))
        private BusinessHours businessHours;

        @ElementCollection
        @CollectionTable(name = "allowed_days_of_week", joinColumns = @JoinColumn(name = "fast_check_config_id"))
        @Column(name = "day_of_week", length = 10)
        private List<String> allowedDaysOfWeek = new ArrayList<>();

        @Column(name = "exclude_holidays")
        private boolean excludeHolidays;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "fast_check_config_id")
        private List<FlashSaleWindow> flashSaleWindows = new ArrayList<>();
    }

    @Entity
    @Table(name = "blackout_periods")
    @Getter
    @Setter
    public static class BlackoutPeriod {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "name", length = 100)
        private String name;

        @Column(name = "start_time")
        private Instant start;

        @Column(name = "end_time")
        private Instant end;

        @Column(name = "reason", length = 500)
        private String reason;
    }

    @Embeddable
    @Getter
    @Setter
    public static class BusinessHours {
        @Column(name = "start_time")
        private LocalTime start;

        @Column(name = "end_time")
        private LocalTime end;

        @Column(name = "timezone", length = 50)
        private String timezone;
    }

    @Entity
    @Table(name = "flash_sale_windows")
    @Getter
    @Setter
    public static class FlashSaleWindow {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "name", length = 100)
        private String name;

        @Column(name = "daily_start")
        private LocalTime dailyStart;

        @Column(name = "daily_end")
        private LocalTime dailyEnd;

        @ElementCollection
        @CollectionTable(name = "flash_sale_active_days", joinColumns = @JoinColumn(name = "flash_sale_window_id"))
        @Column(name = "active_day", length = 10)
        private List<String> activeDays = new ArrayList<>();
    }

    @Embeddable
    @Getter
    @Setter
    public static class OrderConstraints {
        @Column(name = "min_order_value")
        private Long minOrderValue;

        @Column(name = "max_order_value")
        private Long maxOrderValue;

        @ElementCollection
        @CollectionTable(name = "allowed_currencies", joinColumns = @JoinColumn(name = "fast_check_config_id"))
        @Column(name = "currency", length = 10)
        private List<String> allowedCurrencies = new ArrayList<>();

        @Column(name = "min_items")
        private Integer minItems;

        @Column(name = "max_items")
        private Integer maxItems;

        @ElementCollection
        @CollectionTable(name = "required_categories", joinColumns = @JoinColumn(name = "fast_check_config_id"))
        @Column(name = "category", length = 100)
        private List<String> requiredCategories = new ArrayList<>();

        @ElementCollection
        @CollectionTable(name = "excluded_categories", joinColumns = @JoinColumn(name = "fast_check_config_id"))
        @Column(name = "category", length = 100)
        private List<String> excludedCategories = new ArrayList<>();
    }

    @Embeddable
    @Getter
    @Setter
    public static class CustomerConstraints {
        @ElementCollection
        @CollectionTable(name = "allowed_segments", joinColumns = @JoinColumn(name = "fast_check_config_id"))
        @Column(name = "segment", length = 100)
        private List<String> allowedSegments = new ArrayList<>();

        @ElementCollection
        @CollectionTable(name = "excluded_segments", joinColumns = @JoinColumn(name = "fast_check_config_id"))
        @Column(name = "segment", length = 100)
        private List<String> excludedSegments = new ArrayList<>();

        @ElementCollection
        @CollectionTable(name = "allowed_regions", joinColumns = @JoinColumn(name = "fast_check_config_id"))
        @Column(name = "region", length = 100)
        private List<String> allowedRegions = new ArrayList<>();

        @ElementCollection
        @CollectionTable(name = "excluded_regions", joinColumns = @JoinColumn(name = "fast_check_config_id"))
        @Column(name = "region", length = 100)
        private List<String> excludedRegions = new ArrayList<>();

        @Column(name = "require_all_segments")
        private boolean requireAllSegments;

        @Column(name = "max_order_count")
        private Integer maxOrderCount;
    }

    @Embeddable
    @Getter
    @Setter
    public static class RateLimitConfig {
        @Column(name = "max_per_hour")
        private Integer maxPerHour;

        @Column(name = "max_per_day")
        private Integer maxPerDay;

        @Column(name = "max_per_week")
        private Integer maxPerWeek;

        @Column(name = "max_per_month")
        private Integer maxPerMonth;

        @Column(name = "window_type", length = 20) // SLIDING or FIXED
        private String windowType;
    }
}