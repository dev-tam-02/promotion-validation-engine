package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "bundles",
        indexes = {
                @Index(name = "idx_bundle_hash", columnList = "id", unique = true),
                @Index(name = "idx_rule_version", columnList = "rule_id, rule_version")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bundle_hash", columnNames = {"id"})
        }
)
@Getter
@Setter
public class BundleEntity {

    @Id
    @Column(name = "id", nullable = false, length = 300) // bundleHash (sha256 hash format)
    private String id;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "rule_version", nullable = false)
    private Integer ruleVersion;

    @Column(name = "operators_fingerprint", length = 255)
    private String operatorsFingerprint;

    @Embedded
    private EngineInfo engine;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "bundle")
    private List<TimeLinkEntity> timeLinks;

    @Embedded
    private Limits limits;

    @Embedded
    private Artifact artifact;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Embedded
    private Source source;

    @Lob
    @Column(name = "drl_content", columnDefinition = "TEXT")
    private String drlContent;

    @Embeddable
    @Getter
    @Setter
    public static class EngineInfo {
        @Column(name = "engine_type", length = 50)
        private String type;

        @Column(name = "compiler_id", length = 100)
        private String compilerId;

        @Column(name = "drools_version", length = 20)
        private String droolsVersion;
    }

    @Embeddable
    @Getter
    @Setter
    public static class Limits {
        @Column(name = "limit_per_customer")
        private Integer perCustomer;

        @Column(name = "limit_per_day")
        private Integer perDay;
    }

    @Embeddable
    @Getter
    @Setter
    public static class Artifact {
        @Column(name = "artifact_store", length = 50)
        private String store;

        @Column(name = "artifact_key", length = 255)
        private String key;

        @Column(name = "artifact_size")
        private Long size;
    }

    @Embeddable
    @Getter
    @Setter
    public static class Source {
        @Column(name = "validation_rule_version_id", length = 100)
        private String validationRuleVersionId;

        @Column(name = "snapshot_hash", length = 255)
        private String snapshotHash;
    }
}