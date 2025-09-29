package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.EngineConfig;

import java.util.Optional;

@Repository
public interface EngineConfigRepositoryAdapter extends MongoRepository<EngineConfig, String> {

    Optional<EngineConfig> findByTenantId(String tenantId);

    void deleteByTenantId(String tenantId);
}