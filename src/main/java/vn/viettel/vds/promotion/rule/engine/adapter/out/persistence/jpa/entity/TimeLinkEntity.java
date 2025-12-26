package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bundle_time_links")
@Getter
@Setter
public class TimeLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "policy_id", length = 100)
    private String policyId;

    @Column(name = "mode", length = 50)
    private String mode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id", referencedColumnName = "id")
    private BundleEntity bundle;
}