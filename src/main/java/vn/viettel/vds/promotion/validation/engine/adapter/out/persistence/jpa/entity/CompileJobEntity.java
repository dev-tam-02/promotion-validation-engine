package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compile_jobs",
        indexes = {
                @Index(name = "idx_status_time", columnList = "tenant_id, status, requested_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rule_target", columnNames = {"tenant_id", "rule_id", "target_version"})
        }
)
@Getter
@Setter
public class CompileJobEntity {

    @Id
    @Column(name = "id", nullable = false, length = 200) // pj_tenantId_ruleId_targetVersion format
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;

    @Column(name = "target_version", nullable = false)
    private Integer targetVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "operators_fingerprint", length = 255)
    private String operatorsFingerprint;

    @Embedded
    private EngineInfo engine;

    @Column(name = "bundle_hash", length = 300)
    private String bundleHash;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "compileJob")
    private List<LogEntryEntity> logs = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "compile_job_errors", joinColumns = @JoinColumn(name = "compile_job_id"))
    @Column(name = "error_message", length = 1000)
    private List<String> errors = new ArrayList<>();

    public enum JobStatus {
        RUNNING,
        SUCCESS,
        FAILED
    }

    @Embeddable
    @Getter
    @Setter
    public static class EngineInfo {
        @Column(name = "compiler_id", length = 100)
        private String compilerId;
    }
}