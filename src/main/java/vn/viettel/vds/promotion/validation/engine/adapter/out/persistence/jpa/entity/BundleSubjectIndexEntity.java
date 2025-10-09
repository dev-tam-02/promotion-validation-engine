package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bundle_subject_index",
        indexes = {
                @Index(name = "idx_subject", columnList = "tenant_id, subject_type, subject_key"),
                @Index(name = "idx_rule_ver", columnList = "tenant_id, rule_id, rule_version")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant_subject", columnNames = {"tenant_id", "subject_type", "subject_key"})
        }
)
@Getter
@Setter
public class BundleSubjectIndexEntity {

    @Id
    @Column(name = "id", nullable = false, length = 200) // tenantId|type|key format
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Embedded
    private Subject subject;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "rule_version", nullable = false)
    private Integer ruleVersion;

    @Column(name = "assignment_version")
    private Integer assignmentVersion;

    @Column(name = "bundle_hash", length = 300)
    private String bundleHash;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Embeddable
    @Getter
    @Setter
    public static class Subject {
        @Column(name = "subject_type", nullable = false, length = 50)
        private String type;

        @Column(name = "subject_key", nullable = false, length = 100)
        private String key;
    }
}