package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bundle_time_links")
@EntityListeners(IdGenerationListener.class)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TimeLinkEntity extends BaseEntity {

    @Column(name = "policy_id", length = 100)
    private String policyId;

    @Column(name = "mode", length = 50)
    private String mode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id", referencedColumnName = "id")
    private BundleEntity bundle;
}
