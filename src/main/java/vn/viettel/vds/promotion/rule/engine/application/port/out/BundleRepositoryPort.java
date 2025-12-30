package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;

import java.util.List;
import java.util.Optional;

public interface BundleRepositoryPort {

    BundleEntity save(BundleEntity bundle);

    Optional<BundleEntity> findById(String bundleHash);

    Optional<BundleEntity> findByTenantIdAndRuleIdAndRuleVersion(String tenantId, String ruleId, Integer ruleVersion);

    boolean existsById(String bundleHash);
    
    List<BundleEntity> findActiveBundles();
    
    List<BundleEntity> findBundlesByStatus(String status);
}