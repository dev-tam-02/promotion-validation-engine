package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "bundles",
        indexes = {
                @Index(name = "idx_rule_version", columnList = "tenant_id, rule_id, rule_version"),
                @Index(name = "idx_tenant", columnList = "tenant_id")
        }
)
@Getter
@Setter
public class BundleEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "rule_version", nullable = false)
    private Integer ruleVersion;

    @Column(name = "operators_fingerprint", length = 255)
    private String operatorsFingerprint;

    @Embedded
    private EngineInfo engine;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "bundle_id")
    private List<TimeLinkEntity> timeLinks;

    @Embedded
    private Limits limits;

    @Embedded
    private Artifact artifact;

    @Embedded
    private Source source;

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
        @Column(name = "per_customer")
        private Integer perCustomer;

        @Column(name = "per_day")
        private Integer perDay;
    }

    @Embeddable
    @Getter
    @Setter
    public static class Artifact {
        @Column(name = "store", length = 50)
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