package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.EngineConfigEntity;

import java.util.Optional;

@Repository
public interface EngineConfigJpaRepository extends JpaRepository<EngineConfigEntity, String> {

    Optional<EngineConfigEntity> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);

    void deleteByTenantId(String tenantId);
}