package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "assignments",
        indexes = {
                @Index(name = "idx_assignment_lookup", columnList = "subject_type, subject_key, active"),
                @Index(name = "idx_assignment_bundle_hash", columnList = "bundle_hash"),
                @Index(name = "idx_assignment_rule_id", columnList = "rule_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "idx_assignment_subject", columnNames = {"subject_type", "subject_key", "rule_id"})
        }
)
@EntityListeners(IdGenerationListener.class)
@AttributeOverride(name = "id", column = @Column(name = "id", nullable = false, length = 100))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AssignmentEntity extends BaseEntity {

    @Column(name = "subject_type", nullable = false, length = 50)
    private String subjectType;

    @Column(name = "subject_key", nullable = false, length = 200)
    private String subjectKey;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "bundle_hash", length = 300)
    private String bundleHash;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "timezone", length = 50)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(name = "rrule", length = 500)
    private String rrule;

    @Lob
    @Column(name = "time_windows", columnDefinition = "TEXT")
    private String timeWindows;

    @Lob
    @Column(name = "excluded_dates", columnDefinition = "TEXT")
    private String excludedDates;

    @Column(name = "traffic_percent")
    private Integer trafficPercent = 100;

    @Column(name = "sticky_key_strategy", length = 50)
    private String stickyKeyStrategy;

    @Column(name = "included_all")
    private Boolean includedAll = true;

    @Lob
    @Column(name = "included_products", columnDefinition = "TEXT")
    private String includedProducts;

    @Lob
    @Column(name = "excluded_products", columnDefinition = "TEXT")
    private String excludedProducts;

    @Lob
    @Column(name = "included_categories", columnDefinition = "TEXT")
    private String includedCategories;

    @Lob
    @Column(name = "excluded_categories", columnDefinition = "TEXT")
    private String excludedCategories;

    @Lob
    @Column(name = "included_brands", columnDefinition = "TEXT")
    private String includedBrands;

    @Lob
    @Column(name = "excluded_brands", columnDefinition = "TEXT")
    private String excludedBrands;

    @Column(name = "source_version")
    private Long sourceVersion = 0L;

    @Column(name = "tenant_id", length = 100)
    private String tenantId = "";
}
