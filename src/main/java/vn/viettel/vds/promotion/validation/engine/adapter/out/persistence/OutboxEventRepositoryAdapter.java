package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.OutboxEvent;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.repository.OutboxEventMongoRepository;
import vn.viettel.vds.promotion.validation.engine.application.port.out.OutboxEventRepositoryPort;

import java.util.List;
import java.util.Optional;

@Component
public class OutboxEventRepositoryAdapter implements OutboxEventRepositoryPort {

    private final OutboxEventMongoRepository outboxEventMongoRepository;

    public OutboxEventRepositoryAdapter(OutboxEventMongoRepository outboxEventMongoRepository) {
        this.outboxEventMongoRepository = outboxEventMongoRepository;
    }

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return outboxEventMongoRepository.save(outboxEvent);
    }

    @Override
    public Optional<OutboxEvent> findById(String eventId) {
        return outboxEventMongoRepository.findById(eventId);
    }

    @Override
    public List<OutboxEvent> findByTenantIdAndStatusOrderByCreatedAt(String tenantId, OutboxEvent.EventStatus status) {
        return outboxEventMongoRepository.findByTenantIdAndStatusOrderByCreatedAt(tenantId, status);
    }

    @Override
    public List<OutboxEvent> findByStatusOrderByCreatedAt(OutboxEvent.EventStatus status) {
        return outboxEventMongoRepository.findByStatusOrderByCreatedAt(status);
    }

    @Override
    public void deleteByIdIn(List<String> eventIds) {
        outboxEventMongoRepository.deleteByIdIn(eventIds);
    }
}