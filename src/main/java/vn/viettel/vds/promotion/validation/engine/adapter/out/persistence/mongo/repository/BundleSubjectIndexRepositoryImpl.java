package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.BundleSubjectIndex;
import vn.viettel.vds.promotion.validation.engine.application.port.out.BundleSubjectIndexRepositoryPort;

import java.time.Instant;
import java.util.Optional;

@Repository
public class BundleSubjectIndexRepositoryImpl implements BundleSubjectIndexRepositoryPort {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BundleSubjectIndexRepositoryAdapter repository;

    @Override
    public BundleSubjectIndex save(BundleSubjectIndex bundleSubjectIndex) {
        return repository.save(bundleSubjectIndex);
    }

    @Override
    public Optional<BundleSubjectIndex> findByTenantIdAndSubjectTypeAndSubjectKey(
            String tenantId, String subjectType, String subjectKey) {
        return repository.findByTenantIdAndSubjectTypeAndSubjectKey(tenantId, subjectType, subjectKey);
    }

    @Override
    public BundleSubjectIndex upsert(BundleSubjectIndex bundleSubjectIndex) {
        Query query = new Query(Criteria.where("tenantId").is(bundleSubjectIndex.getTenantId())
                .and("subject.type").is(bundleSubjectIndex.getSubject().getType())
                .and("subject.key").is(bundleSubjectIndex.getSubject().getKey()));

        Update update = new Update()
                .set("ruleId", bundleSubjectIndex.getRuleId())
                .set("ruleVersion", bundleSubjectIndex.getRuleVersion())
                .set("assignmentVersion", bundleSubjectIndex.getAssignmentVersion())
                .set("bundleHash", bundleSubjectIndex.getBundleHash())
                .set("updatedAt", Instant.now());

        return mongoTemplate.findAndModify(query, update, BundleSubjectIndex.class);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}