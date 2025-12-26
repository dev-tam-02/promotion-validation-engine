package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bundle_warmups",
        indexes = {
                @Index(name = "idx_tenant_state", columnList = "tenant_id, state, created_at")
        }
)
@Getter
@Setter
public class BundleWarmupEntity {

    @Id
    @Column(name = "id", nullable = false, length = 200) // wu_tenantId_bundleHash format
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "bundle_hash", nullable = false, length = 300)
    private String bundleHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private WarmupState state;

    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum WarmupState {
        QUEUED,
        PROCESSING,
        DONE,
        FAILED
    }
}