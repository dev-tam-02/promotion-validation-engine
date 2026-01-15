package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.EngineConfigEntity;

import java.util.Optional;

public interface EngineConfigRepositoryPort {

    EngineConfigEntity save(EngineConfigEntity engineConfig);

    Optional<EngineConfigEntity> findById(String id);

    EngineConfigEntity upsert(EngineConfigEntity engineConfig);

    void deleteById(String id);
}