package vn.viettel.vds.promotion.rule.engine.adapter.out.events.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration for publishing events.
 */
@Configuration
public class KafkaProducerConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerConfig.class);

    @Value("${promix.messaging.kafka.bootstrap-servers:kafka-1:19092,kafka-2:19093,kafka-3:19094}")
    private String bootstrapServers;

    @Value("${promix.messaging.kafka.client-id:validation-engine}")
    private String clientId;

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

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Basic Kafka properties
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId + "-producer");

        // Serializers
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Producer reliability settings
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Performance settings
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        // JSON serializer settings
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        // SASL authentication if enabled
        if (saslEnabled && saslUsername != null && !saslUsername.isEmpty()) {
            props.put("security.protocol", securityProtocol);
            props.put("sasl.mechanism", saslMechanism);
            String jaasConfig = String.format(
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    saslUsername, saslPassword);
            props.put("sasl.jaas.config", jaasConfig);
            logger.info("Kafka producer configured with SASL authentication");
        }

        logger.info("Kafka producer factory initialized with bootstrap servers: {}", bootstrapServers);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        logger.info("KafkaTemplate bean created");
        return template;
    }
}
