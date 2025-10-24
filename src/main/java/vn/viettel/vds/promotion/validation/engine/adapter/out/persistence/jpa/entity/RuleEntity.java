package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "validation_rules",
        indexes = {
                @Index(name = "idx_enabled", columnList = "enabled")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rule_name", columnNames = {"name"})
        }
)
@Getter
@Setter
public class RuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "drl_text", columnDefinition = "TEXT")
    private String drlText;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;
}