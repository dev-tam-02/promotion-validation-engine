package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.BundleSubjectIndex;

import java.util.Optional;

@Repository
public interface BundleSubjectIndexRepositoryAdapter extends MongoRepository<BundleSubjectIndex, String> {

    Optional<BundleSubjectIndex> findByTenantIdAndSubjectTypeAndSubjectKey(
            String tenantId, String subjectType, String subjectKey);
}