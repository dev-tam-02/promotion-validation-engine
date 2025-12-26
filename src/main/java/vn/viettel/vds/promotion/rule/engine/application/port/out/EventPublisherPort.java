package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.engine.event.BundleCacheInvalidationEvent;
import vn.viettel.vds.promotion.engine.event.BundlePublishedEvent;
import vn.viettel.vds.promotion.engine.event.WarmupRequestedEvent;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.OutboxEventEntity;

/**
 * Port for publishing events from the validation engine.
 * Events are defined in the schema module for centralized management.
 */
public interface EventPublisherPort {

    void publishBundlePublished(BundlePublishedEvent event);

    void publishWarmupRequested(WarmupRequestedEvent event);

    void publishOutboxEvent(OutboxEventEntity outboxEvent);

    /**
     * Publish cache invalidation event to all validation-engine instances.
     * Uses Kafka broadcast pattern (unique group-id per instance).
     */
    void publishCacheInvalidation(BundleCacheInvalidationEvent event);
}