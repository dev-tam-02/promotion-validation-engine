package vn.viettel.vds.promotion.rule.engine.application.dto;

import java.util.Map;

public class HealthResponse {

    private String status;
    private CacheHealth cache;
    private JvmHealth jvm;
    private DatabaseHealth database;
    private ObjectStorageHealth objectStorage;
    private EngineHealth engine;

    public HealthResponse() {
    }

    public HealthResponse(String status, CacheHealth cache, JvmHealth jvm, DatabaseHealth database,
                          ObjectStorageHealth objectStorage, EngineHealth engine) {
        this.status = status;
        this.cache = cache;
        this.jvm = jvm;
        this.database = database;
        this.objectStorage = objectStorage;
        this.engine = engine;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public CacheHealth getCache() {
        return cache;
    }

    public void setCache(CacheHealth cache) {
        this.cache = cache;
    }

    public JvmHealth getJvm() {
        return jvm;
    }

    public void setJvm(JvmHealth jvm) {
        this.jvm = jvm;
    }

    public DatabaseHealth getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseHealth database) {
        this.database = database;
    }

    public ObjectStorageHealth getObjectStorage() {
        return objectStorage;
    }

    public void setObjectStorage(ObjectStorageHealth objectStorage) {
        this.objectStorage = objectStorage;
    }

    public EngineHealth getEngine() {
        return engine;
    }

    public void setEngine(EngineHealth engine) {
        this.engine = engine;
    }

    // Nested classes
    public static class CacheHealth {
        private String status;
        private Double hitRate;
        private Long size;
        private Long evictions;

        public CacheHealth() {
        }

        public CacheHealth(String status, Double hitRate, Long size, Long evictions) {
            this.status = status;
            this.hitRate = hitRate;
            this.size = size;
            this.evictions = evictions;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Double getHitRate() {
            return hitRate;
        }

        public void setHitRate(Double hitRate) {
            this.hitRate = hitRate;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public Long getEvictions() {
            return evictions;
        }

        public void setEvictions(Long evictions) {
            this.evictions = evictions;
        }
    }

    public static class JvmHealth {
        private String status;
        private Long heapUsed;
        private Long heapMax;
        private Long nonHeapUsed;
        private Map<String, Object> gc;

        public JvmHealth() {
        }

        public JvmHealth(String status, Long heapUsed, Long heapMax, Long nonHeapUsed, Map<String, Object> gc) {
            this.status = status;
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.nonHeapUsed = nonHeapUsed;
            this.gc = gc;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getHeapUsed() {
            return heapUsed;
        }

        public void setHeapUsed(Long heapUsed) {
            this.heapUsed = heapUsed;
        }

        public Long getHeapMax() {
            return heapMax;
        }

        public void setHeapMax(Long heapMax) {
            this.heapMax = heapMax;
        }

        public Long getNonHeapUsed() {
            return nonHeapUsed;
        }

        public void setNonHeapUsed(Long nonHeapUsed) {
            this.nonHeapUsed = nonHeapUsed;
        }

        public Map<String, Object> getGc() {
            return gc;
        }

        public void setGc(Map<String, Object> gc) {
            this.gc = gc;
        }
    }

    public static class DatabaseHealth {
        private String status;
        private Boolean connected;
        private Integer connections;
        private Long responseTimeMs;

        public DatabaseHealth() {
        }

        public DatabaseHealth(String status, Boolean connected, Integer connections, Long responseTimeMs) {
            this.status = status;
            this.connected = connected;
            this.connections = connections;
            this.responseTimeMs = responseTimeMs;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Boolean getConnected() {
            return connected;
        }

        public void setConnected(Boolean connected) {
            this.connected = connected;
        }

        public Integer getConnections() {
            return connections;
        }

        public void setConnections(Integer connections) {
            this.connections = connections;
        }

        public Long getResponseTimeMs() {
            return responseTimeMs;
        }

        public void setResponseTimeMs(Long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
        }
    }

    public static class ObjectStorageHealth {
        private String status;
        private Boolean connected;
        private Long responseTimeMs;

        public ObjectStorageHealth() {
        }

        public ObjectStorageHealth(String status, Boolean connected, Long responseTimeMs) {
            this.status = status;
            this.connected = connected;
            this.responseTimeMs = responseTimeMs;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Boolean getConnected() {
            return connected;
        }

        public void setConnected(Boolean connected) {
            this.connected = connected;
        }

        public Long getResponseTimeMs() {
            return responseTimeMs;
        }

        public void setResponseTimeMs(Long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
        }
    }

    public static class EngineHealth {
        private String status;
        private Integer sessionsActive;
        private Long bundlesCached;
        private String droolsVersion;

        public EngineHealth() {
        }

        public EngineHealth(String status, Integer sessionsActive, Long bundlesCached, String droolsVersion) {
            this.status = status;
            this.sessionsActive = sessionsActive;
            this.bundlesCached = bundlesCached;
            this.droolsVersion = droolsVersion;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getSessionsActive() {
            return sessionsActive;
        }

        public void setSessionsActive(Integer sessionsActive) {
            this.sessionsActive = sessionsActive;
        }

        public Long getBundlesCached() {
            return bundlesCached;
        }

        public void setBundlesCached(Long bundlesCached) {
            this.bundlesCached = bundlesCached;
        }

        public String getDroolsVersion() {
            return droolsVersion;
        }

        public void setDroolsVersion(String droolsVersion) {
            this.droolsVersion = droolsVersion;
        }
    }
}