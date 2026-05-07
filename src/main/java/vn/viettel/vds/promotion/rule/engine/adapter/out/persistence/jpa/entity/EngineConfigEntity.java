package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "engine_configs")
@EntityListeners(IdGenerationListener.class)
@AttributeOverride(name = "id", column = @Column(name = "id", nullable = false, length = 100))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EngineConfigEntity extends BaseEntity {

    @Embedded
    @SuppressWarnings("java:S1948")
    private ExecuteConfig execute;

    @Embedded
    @SuppressWarnings("java:S1948")
    private CompileConfig compile;

    @Embeddable
    @Getter
    @Setter
    public static class ExecuteConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

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
    public static class CompileConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "max_nodes")
        private Integer maxNodes;

        @Column(name = "max_depth")
        private Integer maxDepth;
    }
}
