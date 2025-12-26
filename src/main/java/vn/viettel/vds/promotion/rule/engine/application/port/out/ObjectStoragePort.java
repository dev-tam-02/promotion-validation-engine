package vn.viettel.vds.promotion.rule.engine.application.port.out;

import java.util.Optional;

public interface ObjectStoragePort {

    String store(String key, byte[] data);

    Optional<byte[]> retrieve(String key);

    boolean exists(String key);

    void delete(String key);

    public static class StorageMetadata {
        private String key;
        private Long size;
        private String etag;

        public StorageMetadata() {
        }

        public StorageMetadata(String key, Long size, String etag) {
            this.key = key;
            this.size = size;
            this.etag = etag;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public String getEtag() {
            return etag;
        }

        public void setEtag(String etag) {
            this.etag = etag;
        }
    }
}