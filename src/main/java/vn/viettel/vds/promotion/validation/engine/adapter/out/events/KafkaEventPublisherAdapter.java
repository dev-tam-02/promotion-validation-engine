package vn.viettel.vds.promotion.validation.engine.adapter.out.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.OutboxEvent;
import vn.viettel.vds.promotion.validation.engine.application.port.out.EventPublisherPort;

@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisherAdapter.class);

    @Override
    public void publishBundlePublished(BundlePublishedEvent event) {
        logger.info("Publishing BundlePublished event: tenantId={}, ruleId={}, bundleHash={}",
                event.getTenantId(), event.getRuleId(), event.getBundleHash());

        // TODO: Implement actual Kafka publishing using promix-platform starters
        // Example: kafkaTemplate.send("bundle-published", event);
    }

    @Override
    public void publishWarmupRequested(WarmupRequestedEvent event) {
        logger.info("Publishing WarmupRequested event: tenantId={}, bundleHash={}",
                event.getTenantId(), event.getBundleHash());

        // TODO: Implement actual Kafka publishing
        // Example: kafkaTemplate.send("warmup-requested", event);
    }

    @Override
    public void publishOutboxEvent(OutboxEvent outboxEvent) {
        logger.info("Publishing outbox event: id={}, type={}, tenantId={}",
                outboxEvent.getId(), outboxEvent.getType(), outboxEvent.getTenantId());

        switch (outboxEvent.getType()) {
            case BUNDLE_PUBLISHED:
                // Extract payload and publish as BundlePublishedEvent
                break;
            case WARMUP_REQUESTED:
                // Extract payload and publish as WarmupRequestedEvent
                break;
        }

        // TODO: Implement actual event publishing and update outbox status
    }
}