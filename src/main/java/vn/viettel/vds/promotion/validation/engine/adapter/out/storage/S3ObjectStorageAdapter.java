package vn.viettel.vds.promotion.validation.engine.adapter.out.storage;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.application.port.out.ObjectStoragePort;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    // In-memory storage for demo purposes - replace with actual S3 implementation
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public String store(String key, byte[] data) {
        storage.put(key, data);
        return key;
    }

    @Override
    public Optional<byte[]> retrieve(String key) {
        return Optional.ofNullable(storage.get(key));
    }

    @Override
    public boolean exists(String key) {
        return storage.containsKey(key);
    }

    @Override
    public void delete(String key) {
        storage.remove(key);
    }
}