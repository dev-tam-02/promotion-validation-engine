package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.BundleSubjectIndex;

import java.util.Optional;

public interface BundleSubjectIndexRepositoryPort {

    BundleSubjectIndex save(BundleSubjectIndex bundleSubjectIndex);

    Optional<BundleSubjectIndex> findByTenantIdAndSubjectTypeAndSubjectKey(
            String tenantId, String subjectType, String subjectKey);

    BundleSubjectIndex upsert(BundleSubjectIndex bundleSubjectIndex);

    void deleteById(String id);
}