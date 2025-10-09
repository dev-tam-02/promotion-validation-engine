package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.BundleEntity;

import java.util.Optional;

public interface BundleRepositoryPort {

    BundleEntity save(BundleEntity bundle);

    Optional<BundleEntity> findById(String bundleHash);

    Optional<BundleEntity> findByTenantIdAndRuleIdAndRuleVersion(String tenantId, String ruleId, Integer ruleVersion);

    boolean existsById(String bundleHash);
}