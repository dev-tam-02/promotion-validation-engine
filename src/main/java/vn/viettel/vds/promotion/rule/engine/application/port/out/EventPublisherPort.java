package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.engine.event.BundleCacheInvalidationEvent;

/**
 * Port for publishing realtime broadcast events from the rule engine.
 * Business events (e.g. BundlePublished) flow through the Promix outbox starter; this port
 * only handles in-process broadcast signals that bypass the outbox.
 */
public interface EventPublisherPort {

    /**
     * Publish cache invalidation event to all validation-engine instances.
     * Uses Kafka broadcast pattern (unique group-id per instance).
     */
    void publishCacheInvalidation(BundleCacheInvalidationEvent event);
}
