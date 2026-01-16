package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.OutboxEventEntity;

import java.util.List;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, String> {

    List<OutboxEventEntity> findByStatusOrderByCreatedAt(OutboxEventEntity.EventStatus status);

    void deleteByIdIn(List<String> eventIds);
}