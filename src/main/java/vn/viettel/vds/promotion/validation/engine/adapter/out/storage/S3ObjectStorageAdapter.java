package vn.viettel.vds.promotion.validation.engine.adapter.out.storage;

import io.minio.*;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.application.port.out.ObjectStoragePort;
import vn.viettel.vds.promotion.validation.engine.config.MinioProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

/**
 * MinIO implementation of ObjectStoragePort for persistent bundle storage.
 */
@Component
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private static final Logger logger = LoggerFactory.getLogger(S3ObjectStorageAdapter.class);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public S3ObjectStorageAdapter(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public String store(String key, byte[] data) {
        try {
            String bucketName = minioProperties.getBucketName();
            logger.debug("Storing object to MinIO: bucket={}, key={}, size={} bytes",
                        bucketName, key, data.length);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType("application/octet-stream")
                            .build()
            );

            logger.info("Successfully stored object to MinIO: key={}, size={} bytes", key, data.length);
            return key;

        } catch (Exception e) {
            logger.error("Failed to store object to MinIO: key={}", key, e);
            throw new StorageException("Failed to store object: " + key, e);
        }
    }

    @Override
    public Optional<byte[]> retrieve(String key) {
        try {
            String bucketName = minioProperties.getBucketName();
            logger.debug("Retrieving object from MinIO: bucket={}, key={}", bucketName, key);

            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build()
            )) {
                byte[] data = stream.readAllBytes();
                logger.debug("Successfully retrieved object from MinIO: key={}, size={} bytes", key, data.length);
                return Optional.of(data);
            }

        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                logger.debug("Object not found in MinIO: key={}", key);
                return Optional.empty();
            }
            logger.error("Failed to retrieve object from MinIO: key={}", key, e);
            throw new StorageException("Failed to retrieve object: " + key, e);

        } catch (Exception e) {
            logger.error("Failed to retrieve object from MinIO: key={}", key, e);
            throw new StorageException("Failed to retrieve object: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            String bucketName = minioProperties.getBucketName();
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build()
            );
            return true;

        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            logger.error("Failed to check object existence in MinIO: key={}", key, e);
            throw new StorageException("Failed to check object existence: " + key, e);

        } catch (Exception e) {
            logger.error("Failed to check object existence in MinIO: key={}", key, e);
            throw new StorageException("Failed to check object existence: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            String bucketName = minioProperties.getBucketName();
            logger.debug("Deleting object from MinIO: bucket={}, key={}", bucketName, key);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build()
            );

            logger.info("Successfully deleted object from MinIO: key={}", key);

        } catch (Exception e) {
            logger.error("Failed to delete object from MinIO: key={}", key, e);
            throw new StorageException("Failed to delete object: " + key, e);
        }
    }

    /**
     * Custom exception for storage operations.
     */
    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}