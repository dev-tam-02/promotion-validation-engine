package vn.viettel.vds.promotion.rule.engine.adapter.out.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.engine.event.BundleCacheInvalidationEvent;
import vn.viettel.vds.promotion.rule.engine.adapter.in.messaging.config.BroadcastKafkaConfig;
import vn.viettel.vds.promotion.rule.engine.application.port.out.EventPublisherPort;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisherAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BroadcastKafkaConfig broadcastKafkaConfig;

    @Value("${kafka.topics.bundle-cache-invalidation:promotion_bundle_cache_invalidation}")
    private String cacheInvalidationTopic;

    public KafkaEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                      BroadcastKafkaConfig broadcastKafkaConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.broadcastKafkaConfig = broadcastKafkaConfig;
    }

    @Override
    public void publishCacheInvalidation(BundleCacheInvalidationEvent event) {
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
