package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.BundleJpaRepository;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;

import java.util.List;
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
    public Optional<BundleEntity> findByRuleIdAndRuleVersion(String ruleId, Integer ruleVersion) {
        return bundleJpaRepository.findByRuleIdAndRuleVersion(ruleId, ruleVersion);
    }

    @Override
    public boolean existsById(String bundleHash) {
        return bundleJpaRepository.existsById(bundleHash);
    }

    @Override
    public List<BundleEntity> findActiveBundles() {
        return bundleJpaRepository.findByEnabledTrue();
    }

    @Override
    public List<BundleEntity> findBundlesByStatus(String status) {
        boolean enabled = "ACTIVE".equalsIgnoreCase(status);
        return bundleJpaRepository.findByEnabled(enabled);
    }
}