package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.EngineConfig;

import java.util.Optional;

public interface EngineConfigRepositoryPort {

    EngineConfig save(EngineConfig engineConfig);

    Optional<EngineConfig> findByTenantId(String tenantId);

    EngineConfig upsert(EngineConfig engineConfig);

    void deleteByTenantId(String tenantId);
}