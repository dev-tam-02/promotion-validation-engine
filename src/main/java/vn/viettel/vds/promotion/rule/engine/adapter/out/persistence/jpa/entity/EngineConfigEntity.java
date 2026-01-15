package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "engine_configs")
@Getter
@Setter
public class EngineConfigEntity {

    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

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