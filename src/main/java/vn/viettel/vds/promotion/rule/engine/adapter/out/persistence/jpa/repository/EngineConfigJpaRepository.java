package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.EngineConfigEntity;

@Repository
public interface EngineConfigJpaRepository extends JpaRepository<EngineConfigEntity, String> {
    // Uses JpaRepository default methods: findById, existsById, deleteById
}