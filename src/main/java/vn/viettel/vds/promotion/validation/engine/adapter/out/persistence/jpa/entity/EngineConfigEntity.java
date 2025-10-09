package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "engine_configs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant", columnNames = {"tenant_id"})
        }
)
@Getter
@Setter
public class EngineConfigEntity {

    @Id
    @Column(name = "id", nullable = false, length = 100) // cfg_tenantId format
    private String id;

    @Column(name = "tenant_id", nullable = false, unique = true, length = 50)
    private String tenantId;

    @Embedded
    private ExecuteConfig execute;

    @Embedded
    private CompileConfig compile;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Embeddable
    @Getter
    @Setter
    public static class ExecuteConfig {
        @Column(name = "timeout_ms")
        private Integer timeoutMs;

        @Column(name = "max_rules_fired")
        private Integer maxRulesFired;

        @Column(name = "max_facts")
        private Integer maxFacts;

        @ElementCollection
        @CollectionTable(
                name = "engine_config_explain_sampling",
                joinColumns = @JoinColumn(name = "engine_config_id")
        )
        @MapKeyColumn(name = "sampling_key", length = 100)
        @Column(name = "sampling_value")
        private Map<String, Double> explainSampling = new HashMap<>();
    }

    @Embeddable
    @Getter
    @Setter
    public static class CompileConfig {
        @Column(name = "max_nodes")
        private Integer maxNodes;

        @Column(name = "max_depth")
        private Integer maxDepth;
    }
}