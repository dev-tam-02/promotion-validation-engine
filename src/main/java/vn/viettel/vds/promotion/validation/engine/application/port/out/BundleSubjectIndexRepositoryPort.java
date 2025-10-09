package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.BundleSubjectIndexEntity;

import java.util.Optional;

public interface BundleSubjectIndexRepositoryPort {

    BundleSubjectIndexEntity save(BundleSubjectIndexEntity bundleSubjectIndex);

    Optional<BundleSubjectIndexEntity> findByTenantIdAndSubjectTypeAndSubjectKey(
            String tenantId, String subjectType, String subjectKey);

    BundleSubjectIndexEntity upsert(BundleSubjectIndexEntity bundleSubjectIndex);

    void deleteById(String id);
}