package vn.viettel.vds.promotion.rule.engine.adapter.in.messaging.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import vn.viettel.vds.promotion.engine.event.BundleCacheInvalidationEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka configuration for broadcast pattern.
 * Each instance gets a unique group-id so ALL instances receive the same message.
 * This is used for cache invalidation where every instance needs to be notified.
 */
@Configuration
public class BroadcastKafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastKafkaConfig.class);
    private final String instanceId;
    @Value("${spring.application.name:validation-engine}")
    private String applicationName;
    @Value("${promix.messaging.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${promix.messaging.kafka.security.protocol:PLAINTEXT}")
    private String securityProtocol;
    @Value("${promix.messaging.kafka.sasl.mechanism:PLAIN}")
    private String saslMechanism;
    @Value("${promix.messaging.kafka.sasl.username:}")
    private String saslUsername;
    @Value("${promix.messaging.kafka.sasl.password:}")
    private String saslPassword;
    @Value("${promix.messaging.kafka.sasl.enabled:false}")
    private boolean saslEnabled;

    public BroadcastKafkaConfig() {
        this.instanceId = generateInstanceId();
        logger.info("Initialized BroadcastKafkaConfig with instanceId: {}", instanceId);
    }

    /**
     * Generate unique instance ID using hostname + UUID.
     * This ensures each instance has a different group-id for broadcast pattern.
     */
    private String generateInstanceId() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Consumer factory for broadcast messages.
     * Uses unique group-id per instance to ensure all instances receive messages.
     */
    @Bean
    public ConsumerFactory<String, BundleCacheInvalidationEvent> broadcastConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Basic Kafka properties
        logger.info("BroadcastKafkaConfig - bootstrap-servers: {}", bootstrapServers);
        logger.info("BroadcastKafkaConfig - security.protocol: {}, sasl.enabled: {}", securityProtocol, saslEnabled);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // UNIQUE GROUP ID PER INSTANCE - Key for broadcast pattern
        String uniqueGroupId = applicationName + "-broadcast-" + instanceId;
        props.put(ConsumerConfig.GROUP_ID_CONFIG, uniqueGroupId);
        logger.info("Broadcast consumer using unique group-id: {}", uniqueGroupId);

        // Only receive new messages (not historical)
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        // Deserializers
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        // JSON deserializer settings
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // Consumer settings optimized for broadcast
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        // SASL authentication if enabled
        if (saslEnabled && saslUsername != null && !saslUsername.isEmpty()) {
            props.put("security.protocol", securityProtocol);
            props.put("sasl.mechanism", saslMechanism);
            String jaasConfig = String.format(
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    saslUsername, saslPassword);
            props.put("sasl.jaas.config", jaasConfig);
        }

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Listener container factory for broadcast consumers.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BundleCacheInvalidationEvent> broadcastListenerContainerFactory(
            ConsumerFactory<String, BundleCacheInvalidationEvent> broadcastConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, BundleCacheInvalidationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(broadcastConsumerFactory);

        // Single thread is enough for broadcast messages (low volume)
        factory.setConcurrency(1);

        // No batch processing needed
        factory.setBatchListener(false);

        logger.info("Configured broadcastListenerContainerFactory for cache invalidation");

        return factory;
    }
}
