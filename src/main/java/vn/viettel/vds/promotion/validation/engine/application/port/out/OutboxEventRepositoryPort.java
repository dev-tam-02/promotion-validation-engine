package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.OutboxEvent;

import java.util.List;
import java.util.Optional;

public interface OutboxEventRepositoryPort {

    OutboxEvent save(OutboxEvent outboxEvent);

    Optional<OutboxEvent> findById(String eventId);

    List<OutboxEvent> findByTenantIdAndStatusOrderByCreatedAt(
            String tenantId, OutboxEvent.EventStatus status);

    List<OutboxEvent> findByStatusOrderByCreatedAt(OutboxEvent.EventStatus status);

    void deleteByIdIn(List<String> eventIds);
}