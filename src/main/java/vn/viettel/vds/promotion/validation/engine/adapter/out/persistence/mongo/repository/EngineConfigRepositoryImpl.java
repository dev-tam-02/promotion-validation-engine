package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.EngineConfig;
import vn.viettel.vds.promotion.validation.engine.application.port.out.EngineConfigRepositoryPort;

import java.time.Instant;
import java.util.Optional;

@Repository
public class EngineConfigRepositoryImpl implements EngineConfigRepositoryPort {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private EngineConfigRepositoryAdapter repository;

    @Override
    public EngineConfig save(EngineConfig engineConfig) {
        return repository.save(engineConfig);
    }

    @Override
    public Optional<EngineConfig> findByTenantId(String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @Override
    public EngineConfig upsert(EngineConfig engineConfig) {
        Query query = new Query(Criteria.where("tenantId").is(engineConfig.getTenantId()));

        Update update = new Update()
                .set("execute", engineConfig.getExecute())
                .set("compile", engineConfig.getCompile())
                .set("updatedAt", Instant.now());

        return mongoTemplate.findAndModify(query, update, EngineConfig.class);
    }

    @Override
    public void deleteByTenantId(String tenantId) {
        repository.deleteByTenantId(tenantId);
    }
}