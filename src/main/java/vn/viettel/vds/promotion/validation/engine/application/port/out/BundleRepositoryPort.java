package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.Bundle;

import java.util.Optional;

public interface BundleRepositoryPort {

    Bundle save(Bundle bundle);

    Optional<Bundle> findById(String bundleHash);

    Optional<Bundle> findByTenantIdAndRuleIdAndRuleVersion(String tenantId, String ruleId, Integer ruleVersion);

    boolean existsById(String bundleHash);
}