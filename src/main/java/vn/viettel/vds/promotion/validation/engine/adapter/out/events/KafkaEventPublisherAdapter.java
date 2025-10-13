package vn.viettel.vds.promotion.validation.engine.adapter.out.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.OutboxEventEntity;
import vn.viettel.vds.promotion.validation.engine.application.port.out.EventPublisherPort;

@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisherAdapter.class);

    @Override
    public void publishBundlePublished(BundlePublishedEvent event) {
        logger.info("Publishing BundlePublished event: tenantId={}, ruleId={}, bundleHash={}",
                event.getTenantId(), event.getRuleId(), event.getBundleHash());

        // Future enhancement: Implement actual Kafka publishing using promix-platform starters
        // This would involve:
        // 1. Injecting KafkaTemplate from promix-platform messaging starter
        // 2. Serializing the event to appropriate format
        // 3. Sending to configured Kafka topic
    }

    @Override
    public void publishWarmupRequested(WarmupRequestedEvent event) {
        logger.info("Publishing WarmupRequested event: tenantId={}, bundleHash={}",
                event.getTenantId(), event.getBundleHash());

        // Future enhancement: Implement actual Kafka publishing
        // Similar to publishBundlePublished but for warmup requests
    }

    @Override
    public void publishOutboxEvent(OutboxEventEntity outboxEvent) {
        logger.info("Publishing outbox event: id={}, type={}, tenantId={}",
                outboxEvent.getId(), outboxEvent.getType(), outboxEvent.getTenantId());

        if (OutboxEventEntity.EventType.BUNDLE_PUBLISHED.equals(outboxEvent.getType())) {
            // Extract payload and publish as BundlePublishedEvent
        }

        // Future enhancement: Implement actual event publishing and update outbox status
    }
}