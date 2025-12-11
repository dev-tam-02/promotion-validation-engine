package vn.viettel.vds.promotion.validation.engine.adapter.out.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.engine.event.BundleCacheInvalidationEvent;
import vn.viettel.vds.promotion.engine.event.BundlePublishedEvent;
import vn.viettel.vds.promotion.engine.event.WarmupRequestedEvent;
import vn.viettel.vds.promotion.validation.engine.adapter.in.messaging.config.BroadcastKafkaConfig;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.OutboxEventEntity;
import vn.viettel.vds.promotion.validation.engine.application.port.out.EventPublisherPort;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisherAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BroadcastKafkaConfig broadcastKafkaConfig;

    @Value("${kafka.topics.bundle-published:promotion_bundle_published}")
    private String bundlePublishedTopic;

    @Value("${kafka.topics.warmup-requested:promotion_warmup_requested}")
    private String warmupRequestedTopic;

    @Value("${kafka.topics.bundle-cache-invalidation:promotion_bundle_cache_invalidation}")
    private String cacheInvalidationTopic;

    public KafkaEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                      BroadcastKafkaConfig broadcastKafkaConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.broadcastKafkaConfig = broadcastKafkaConfig;
    }

    @Override
    public void publishBundlePublished(BundlePublishedEvent event) {
        logger.info("Publishing BundlePublished event: tenantId={}, ruleId={}, bundleHash={}",
                event.getTenantId(), event.getRuleId(), event.getBundleHash());

        sendToKafka(bundlePublishedTopic, event.getBundleHash(), event);
    }

    @Override
    public void publishWarmupRequested(WarmupRequestedEvent event) {
        logger.info("Publishing WarmupRequested event: tenantId={}, bundleHash={}",
                event.getTenantId(), event.getBundleHash());

        sendToKafka(warmupRequestedTopic, event.getBundleHash(), event);
    }

    @Override
    public void publishOutboxEvent(OutboxEventEntity outboxEvent) {
        logger.info("Publishing outbox event: id={}, type={}, tenantId={}",
                outboxEvent.getId(), outboxEvent.getType(), outboxEvent.getTenantId());

        if (OutboxEventEntity.EventType.BUNDLE_PUBLISHED.equals(outboxEvent.getType())) {
            String ruleId = outboxEvent.getPayload().get("ruleId");
            String bundleHash = outboxEvent.getPayload().get("bundleHash");
            Integer ruleVersion = outboxEvent.getPayload().get("ruleVersion") != null
                    ? Integer.parseInt(outboxEvent.getPayload().get("ruleVersion"))
                    : null;

            BundlePublishedEvent event = new BundlePublishedEvent(
                    outboxEvent.getTenantId(), ruleId, ruleVersion, null, bundleHash);
            publishBundlePublished(event);
        }
    }

    @Override
    public void publishCacheInvalidation(BundleCacheInvalidationEvent event) {
        // Set source instance ID so receiving instances can skip self-processing
        event.setSourceInstanceId(broadcastKafkaConfig.getInstanceId());

        logger.info("Publishing cache invalidation event: eventId={}, bundleHash={}, type={}, sourceInstance={}",
                event.getEventId(), event.getBundleHash(), event.getType(), event.getSourceInstanceId());

        sendToKafka(cacheInvalidationTopic, event.getBundleHash(), event);
    }

    private void sendToKafka(String topic, String key, Object event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send event to Kafka: topic={}, key={}, error={}",
                            topic, key, ex.getMessage(), ex);
                } else {
                    logger.debug("Successfully sent event to Kafka: topic={}, key={}, partition={}, offset={}",
                            topic, key,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            logger.error("Error sending event to Kafka: topic={}, key={}", topic, key, e);
        }
    }
}