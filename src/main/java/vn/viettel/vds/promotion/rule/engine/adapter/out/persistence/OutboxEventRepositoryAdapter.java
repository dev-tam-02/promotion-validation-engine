package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.OutboxEventEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.OutboxEventJpaRepository;
import vn.viettel.vds.promotion.rule.engine.application.port.out.OutboxEventRepositoryPort;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepositoryPort {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    public OutboxEventEntity save(OutboxEventEntity outboxEvent) {
        return outboxEventJpaRepository.save(outboxEvent);
    }

    public Optional<OutboxEventEntity> findById(String eventId) {
        return outboxEventJpaRepository.findById(eventId);
    }

    public List<OutboxEventEntity> findByStatusOrderByCreatedAt(OutboxEventEntity.EventStatus status) {
        return outboxEventJpaRepository.findByStatusOrderByCreatedAt(status);
    }

    public void deleteByIdIn(List<String> eventIds) {
        outboxEventJpaRepository.deleteByIdIn(eventIds);
    }
}