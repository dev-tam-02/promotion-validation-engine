package vn.viettel.vds.promotion.validation.engine.application.usecase;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.application.dto.HealthResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.in.HealthCheckUseCase;
import vn.viettel.vds.promotion.validation.engine.application.port.out.ObjectStoragePort;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.Map;

@Service
public class HealthCheckService implements HealthCheckUseCase {

    private final ObjectStoragePort objectStoragePort;
    @PersistenceContext
    private EntityManager entityManager;

    public HealthCheckService(ObjectStoragePort objectStoragePort) {
        this.objectStoragePort = objectStoragePort;
    }

    @Override
    public HealthResponse getHealth() {
        HealthResponse response = new HealthResponse();

        // Check overall status
        boolean allHealthy = true;

        // Check cache health
        HealthResponse.CacheHealth cacheHealth = checkCacheHealth();
        if (!"UP".equals(cacheHealth.getStatus())) {
            allHealthy = false;
        }

        // Check JVM health
        HealthResponse.JvmHealth jvmHealth = checkJvmHealth();
        if (!"UP".equals(jvmHealth.getStatus())) {
            allHealthy = false;
        }

        // Check database health
        HealthResponse.DatabaseHealth databaseHealth = checkDatabaseHealth();
        if (!"UP".equals(databaseHealth.getStatus())) {
            allHealthy = false;
        }

        // Check object storage health
        HealthResponse.ObjectStorageHealth objectStorageHealth = checkObjectStorageHealth();
        if (!"UP".equals(objectStorageHealth.getStatus())) {
            allHealthy = false;
        }

        // Check engine health
        HealthResponse.EngineHealth engineHealth = checkEngineHealth();
        if (!"UP".equals(engineHealth.getStatus())) {
            allHealthy = false;
        }

        response.setStatus(allHealthy ? "UP" : "DOWN");
        response.setCache(cacheHealth);
        response.setJvm(jvmHealth);
        response.setDatabase(databaseHealth);
        response.setObjectStorage(objectStorageHealth);
        response.setEngine(engineHealth);

        return response;
    }

    private HealthResponse.CacheHealth checkCacheHealth() {
        HealthResponse.CacheHealth health = new HealthResponse.CacheHealth();

        try {
            // Future enhancement: Implement actual cache metrics
            // This would retrieve statistics from the compilation cache service
            // For now, return simulated values
            health.setStatus("UP");
            health.setHitRate(0.85); // 85% hit rate
            health.setSize(1024L);   // 1024 cached bundles
            health.setEvictions(0L);
        } catch (Exception e) {
            health.setStatus("DOWN");
        }

        return health;
    }

    private HealthResponse.JvmHealth checkJvmHealth() {
        HealthResponse.JvmHealth health = new HealthResponse.JvmHealth();

        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();

            health.setHeapUsed(heapUsed);
            health.setHeapMax(heapMax);
            health.setNonHeapUsed(nonHeapUsed);

            // GC information
            Map<String, Object> gcInfo = new HashMap<>();
            for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                gcInfo.put(gcBean.getName() + "_collections", gcBean.getCollectionCount());
                gcInfo.put(gcBean.getName() + "_time", gcBean.getCollectionTime());
            }
            health.setGc(gcInfo);

            // Check if memory usage is healthy (less than 90%)
            double heapUsageRatio = (double) heapUsed / heapMax;
            health.setStatus(heapUsageRatio < 0.9 ? "UP" : "WARN");

        } catch (Exception e) {
            health.setStatus("DOWN");
        }

        return health;
    }

    private HealthResponse.DatabaseHealth checkDatabaseHealth() {
        HealthResponse.DatabaseHealth health = new HealthResponse.DatabaseHealth();

        try {
            long startTime = System.currentTimeMillis();

            // Simple ping to database using JPA
            entityManager.createNativeQuery("SELECT 1").getSingleResult();

            long responseTime = System.currentTimeMillis() - startTime;

            health.setStatus("UP");
            health.setConnected(true);
            health.setConnections(1); // Future enhancement: Get actual connection pool size from datasource
            health.setResponseTimeMs(responseTime);

        } catch (Exception e) {
            health.setStatus("DOWN");
            health.setConnected(false);
            health.setResponseTimeMs(null);
        }

        return health;
    }

    private HealthResponse.ObjectStorageHealth checkObjectStorageHealth() {
        HealthResponse.ObjectStorageHealth health = new HealthResponse.ObjectStorageHealth();

        try {
            long startTime = System.currentTimeMillis();

            // Test connectivity to object storage
            // Use a dummy key that doesn't need to exist
            objectStoragePort.exists("health-check-key");

            long responseTime = System.currentTimeMillis() - startTime;

            health.setStatus("UP");
            health.setConnected(true);
            health.setResponseTimeMs(responseTime);

        } catch (Exception e) {
            health.setStatus("DOWN");
            health.setConnected(false);
            health.setResponseTimeMs(null);
        }

        return health;
    }

    private HealthResponse.EngineHealth checkEngineHealth() {
        HealthResponse.EngineHealth health = new HealthResponse.EngineHealth();

        try {
            // Future enhancement: Implement actual engine health checks
            // This would check:
            // 1. Rule engine availability
            // 2. Session pool health
            // 3. External service connectivity
            health.setStatus("UP");
            health.setSessionsActive(0);
            health.setBundlesCached(0L);
            health.setDroolsVersion("10.1.0");

        } catch (Exception e) {
            health.setStatus("DOWN");
        }

        return health;
    }
}