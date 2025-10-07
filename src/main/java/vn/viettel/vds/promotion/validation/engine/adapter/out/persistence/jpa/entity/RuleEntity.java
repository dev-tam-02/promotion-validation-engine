package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "validation_rules",
        indexes = {
                @Index(name = "idx_rule_name_unique", columnList = "name", unique = true),
                @Index(name = "idx_enabled", columnList = "enabled")
        }
)
@Getter
@Setter
public class RuleEntity extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "rule_version", length = 50)
    private String ruleVersion;

    @Column(name = "drl_text", columnDefinition = "TEXT")
    private String drlText;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}