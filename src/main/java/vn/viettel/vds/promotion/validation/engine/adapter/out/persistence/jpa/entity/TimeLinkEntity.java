package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bundle_time_links")
@Getter
@Setter
public class TimeLinkEntity extends BaseEntity {

    @Column(name = "policy_id", length = 100)
    private String policyId;

    @Column(name = "mode", length = 50)
    private String mode;

    @Column(name = "bundle_id", length = 100)
    private String bundleId;
}