package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.OutboxEventEntity;

import java.util.List;
import java.util.Optional;

public interface OutboxEventRepositoryPort {

    OutboxEventEntity save(OutboxEventEntity outboxEvent);

    Optional<OutboxEventEntity> findById(String eventId);

    List<OutboxEventEntity> findByStatusOrderByCreatedAt(OutboxEventEntity.EventStatus status);

    void deleteByIdIn(List<String> eventIds);
}