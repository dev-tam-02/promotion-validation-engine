package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "fast_check_configs")
@Getter
@Setter
@NoArgsConstructor
public class FastCheckConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "subject_type", nullable = false, length = 50)
    private String subjectType;

    @Column(name = "subject_key", nullable = false, length = 200)
    private String subjectKey;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "business_hours_start")
    private LocalTime businessHoursStart;

    @Column(name = "business_hours_end")
    private LocalTime businessHoursEnd;

    @Column(name = "business_hours_timezone", length = 50)
    private String businessHoursTimezone;

    @Column(name = "allowed_days_of_week_json", columnDefinition = "TEXT")
    private String allowedDaysOfWeekJson;

    @Column(name = "exclude_holidays")
    private Boolean excludeHolidays;

    @Column(name = "min_order_value")
    private Long minOrderValue;

    @Column(name = "max_order_value")
    private Long maxOrderValue;

    @Column(name = "min_items")
    private Integer minItems;

    @Column(name = "max_items")
    private Integer maxItems;

    @Column(name = "allowed_currencies_json", columnDefinition = "TEXT")
    private String allowedCurrenciesJson;

    @Column(name = "required_segments_json", columnDefinition = "TEXT")
    private String requiredSegmentsJson;

    @Column(name = "excluded_segments_json", columnDefinition = "TEXT")
    private String excludedSegmentsJson;

    @Column(name = "require_all_segments")
    private Boolean requireAllSegments;

    @Column(name = "max_order_count")
    private Integer maxOrderCount;

    @Column(name = "max_per_hour")
    private Integer maxPerHour;

    @Column(name = "max_per_day")
    private Integer maxPerDay;

    @Column(name = "max_per_week")
    private Integer maxPerWeek;

    @Column(name = "max_per_month")
    private Integer maxPerMonth;

    @Column(name = "window_type", length = 20)
    private String windowType;

    @Column(name = "source_version")
    private Long sourceVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public void touchCreated() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }
}
