package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.EngineConfigEntity;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository.EngineConfigJpaRepository;
import vn.viettel.vds.promotion.validation.engine.application.port.out.EngineConfigRepositoryPort;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EngineConfigRepositoryAdapter implements EngineConfigRepositoryPort {

    private final EngineConfigJpaRepository engineConfigJpaRepository;

    @Override
    public EngineConfigEntity save(EngineConfigEntity engineConfig) {
        return engineConfigJpaRepository.save(engineConfig);
    }

    @Override
    public Optional<EngineConfigEntity> findByTenantId(String tenantId) {
        return engineConfigJpaRepository.findByTenantId(tenantId);
    }

    @Override
    public EngineConfigEntity upsert(EngineConfigEntity engineConfig) {
        // Check if config exists for this tenant
        Optional<EngineConfigEntity> existingConfig = engineConfigJpaRepository.findByTenantId(engineConfig.getTenantId());

        if (existingConfig.isPresent()) {
            // Update existing config
            EngineConfigEntity existing = existingConfig.get();
            existing.setExecute(engineConfig.getExecute());
            existing.setCompile(engineConfig.getCompile());
            existing.setUpdatedAt(engineConfig.getUpdatedAt());
            return engineConfigJpaRepository.save(existing);
        } else {
            // Insert new config
            return engineConfigJpaRepository.save(engineConfig);
        }
    }

    @Override
    public void deleteByTenantId(String tenantId) {
        engineConfigJpaRepository.deleteByTenantId(tenantId);
    }
}