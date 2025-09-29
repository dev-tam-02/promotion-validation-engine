package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.converter.MapStringObjectConverter;
import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Entity
@Table(name = "engine_configs",
    indexes = {
        @Index(name = "idx_tenant_unique", columnList = "tenant_id", unique = true)
    }
)
@Getter
@Setter
public class EngineConfigEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, unique = true, length = 50)
    private String tenantId;

    @Embedded
    private ExecuteConfig execute;

    @Embedded
    private CompileConfig compile;

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

        @Convert(converter = MapStringObjectConverter.class)
        @Column(name = "explain_sampling", columnDefinition = "TEXT")
        private Map<String, Double> explainSampling;
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