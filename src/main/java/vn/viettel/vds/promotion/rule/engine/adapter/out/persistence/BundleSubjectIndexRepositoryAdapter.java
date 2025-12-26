package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleSubjectIndexEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.BundleSubjectIndexJpaRepository;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleSubjectIndexRepositoryPort;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BundleSubjectIndexRepositoryAdapter implements BundleSubjectIndexRepositoryPort {

    private final BundleSubjectIndexJpaRepository bundleSubjectIndexJpaRepository;

    @Override
    public BundleSubjectIndexEntity save(BundleSubjectIndexEntity bundleSubjectIndex) {
        return bundleSubjectIndexJpaRepository.save(bundleSubjectIndex);
    }

    @Override
    public Optional<BundleSubjectIndexEntity> findByTenantIdAndSubjectTypeAndSubjectKey(
            String tenantId, String subjectType, String subjectKey) {
        return bundleSubjectIndexJpaRepository.findByTenantIdAndSubject(tenantId, subjectType, subjectKey);
    }

    @Override
    public BundleSubjectIndexEntity upsert(BundleSubjectIndexEntity bundleSubjectIndex) {
        // Check if index exists for this tenant and subject
        Optional<BundleSubjectIndexEntity> existingIndex = bundleSubjectIndexJpaRepository.findByTenantIdAndSubject(
                bundleSubjectIndex.getTenantId(),
                bundleSubjectIndex.getSubject().getType(),
                bundleSubjectIndex.getSubject().getKey()
        );

        if (existingIndex.isPresent()) {
            // Update existing index
            BundleSubjectIndexEntity existing = existingIndex.get();
            existing.setBundleHash(bundleSubjectIndex.getBundleHash());
            existing.setRuleId(bundleSubjectIndex.getRuleId());
            existing.setRuleVersion(bundleSubjectIndex.getRuleVersion());
            existing.setUpdatedAt(bundleSubjectIndex.getUpdatedAt());
            return bundleSubjectIndexJpaRepository.save(existing);
        } else {
            // Insert new index
            return bundleSubjectIndexJpaRepository.save(bundleSubjectIndex);
        }
    }

    @Override
    public void deleteById(String id) {
        bundleSubjectIndexJpaRepository.deleteById(id);
    }
}