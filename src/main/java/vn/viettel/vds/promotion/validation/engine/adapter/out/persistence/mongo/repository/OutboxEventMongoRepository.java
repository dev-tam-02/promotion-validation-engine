package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.OutboxEvent;

import java.util.List;

@Repository
public interface OutboxEventMongoRepository extends MongoRepository<OutboxEvent, String> {

    List<OutboxEvent> findByTenantIdAndStatusOrderByCreatedAt(String tenantId, OutboxEvent.EventStatus status);

    List<OutboxEvent> findByStatusOrderByCreatedAt(OutboxEvent.EventStatus status);

    void deleteByIdIn(List<String> eventIds);
}