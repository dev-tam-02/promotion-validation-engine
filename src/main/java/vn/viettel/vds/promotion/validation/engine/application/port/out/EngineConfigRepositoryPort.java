package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.EngineConfigEntity;

import java.util.Optional;

public interface EngineConfigRepositoryPort {

    EngineConfigEntity save(EngineConfigEntity engineConfig);

    Optional<EngineConfigEntity> findByTenantId(String tenantId);

    EngineConfigEntity upsert(EngineConfigEntity engineConfig);

    void deleteByTenantId(String tenantId);
}