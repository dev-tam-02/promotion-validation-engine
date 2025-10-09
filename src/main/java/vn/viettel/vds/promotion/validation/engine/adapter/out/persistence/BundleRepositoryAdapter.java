package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository.BundleJpaRepository;
import vn.viettel.vds.promotion.validation.engine.application.port.out.BundleRepositoryPort;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BundleRepositoryAdapter implements BundleRepositoryPort {

    private final BundleJpaRepository bundleJpaRepository;

    @Override
    public BundleEntity save(BundleEntity bundle) {
        return bundleJpaRepository.save(bundle);
    }

    @Override
    public Optional<BundleEntity> findById(String bundleHash) {
        return bundleJpaRepository.findById(bundleHash);
    }

    @Override
    public Optional<BundleEntity> findByTenantIdAndRuleIdAndRuleVersion(String tenantId, String ruleId, Integer ruleVersion) {
        return bundleJpaRepository.findByTenantIdAndRuleIdAndRuleVersion(tenantId, ruleId, ruleVersion);
    }

    @Override
    public boolean existsById(String bundleHash) {
        return bundleJpaRepository.existsById(bundleHash);
    }
}