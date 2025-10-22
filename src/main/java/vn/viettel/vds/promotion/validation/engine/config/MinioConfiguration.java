package vn.viettel.vds.promotion.validation.engine.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MinIO client and bucket initialization.
 */
@Configuration
@ConditionalOnProperty(prefix = "minio", name = "endpoint")
public class MinioConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MinioConfiguration.class);

    private final MinioProperties minioProperties;

    public MinioConfiguration(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    @Bean
    public MinioClient minioClient() {
        logger.info("Initializing MinIO client with endpoint: {}", minioProperties.getEndpoint());

        MinioClient client = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();

        // Set timeouts
        client.setTimeout(
                minioProperties.getConnectTimeout(),
                minioProperties.getWriteTimeout(),
                minioProperties.getReadTimeout()
        );

        // Auto-create bucket if enabled
        if (minioProperties.isAutoCreateBucket()) {
            createBucketIfNotExists(client);
        }

        logger.info("MinIO client initialized successfully");
        return client;
    }

    private void createBucketIfNotExists(MinioClient client) {
        try {
            String bucketName = minioProperties.getBucketName();
            boolean bucketExists = client.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!bucketExists) {
                logger.info("Bucket '{}' does not exist, creating...", bucketName);
                client.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                logger.info("Bucket '{}' created successfully", bucketName);
            } else {
                logger.info("Bucket '{}' already exists", bucketName);
            }
        } catch (Exception e) {
            logger.error("Failed to create or check bucket: {}", minioProperties.getBucketName(), e);
            throw new IllegalStateException("Could not initialize MinIO bucket", e);
        }
    }
}
