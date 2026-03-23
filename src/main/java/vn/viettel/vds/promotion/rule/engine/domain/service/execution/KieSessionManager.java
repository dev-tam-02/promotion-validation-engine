package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.domain.service.session.KieSessionFactory;
import vn.viettel.vds.promotion.rule.engine.domain.service.session.KieSessionPool;
import vn.viettel.vds.promotion.rule.engine.domain.service.session.SessionPoolConfig;

import java.util.concurrent.TimeUnit;

@Service
public class KieSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(KieSessionManager.class);

    private final Cache<String, KieContainer> containerCache;
    private final KieSessionPool sessionPool;
    private final SessionPoolConfig poolConfig;
    private final KieSessionFactory sessionFactory;

    public KieSessionManager(KieSessionPool sessionPool, SessionPoolConfig poolConfig, KieSessionFactory sessionFactory) {
        this.sessionPool = sessionPool;
        this.poolConfig = poolConfig;
        this.sessionFactory = sessionFactory;
        this.containerCache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(60, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    logger.debug("KieContainer removed from cache: key={}, cause={}", key, cause);
                    if (value instanceof KieContainer container) {
                        try {
                            container.dispose();
                            // Also invalidate session pool for this bundle
                            if (key instanceof String bundleHash) {
                                sessionPool.invalidateBundle(bundleHash);
                            }
                        } catch (Exception e) {
                            logger.warn("Error disposing KieContainer", e);
                        }
                    }
                })
                .build();
    }

    @PostConstruct
    public void initializeSessionFactory() {
        sessionFactory.setContainerCache(containerCache);
    }

    public StatelessKieSession createStatelessSession(KieContainer container) {
        try {
            StatelessKieSession session = container.newStatelessKieSession();
            logger.debug("Created new stateless KIE session for container");
            return session;
        } catch (Exception e) {
            throw new KieSessionCreationException("Failed to create stateless KIE session", e);
        }
    }

    @SuppressWarnings("java:S2095") // Returning session to be managed by caller
    public KieSession createStatefulSession(KieContainer container) {
        try {
            KieSession session = container.newKieSession();
            logger.debug("Created new stateful KIE session for container");
            return session;
        } catch (Exception e) {
            throw new KieSessionCreationException("Failed to create stateful KIE session", e);
        }
    }

    public KieSessionPool.PooledKieSession borrowPooledSession(String bundleHash) {
        if (!poolConfig.isEnablePooling()) {
            // Pooling disabled, create new session
            KieContainer container = getCachedContainer(bundleHash);
            if (container == null) {
                throw new IllegalArgumentException("No cached container found for bundle: " + bundleHash);
            }
            KieSession session = createStatefulSession(container);
            return new KieSessionPool.PooledKieSession(session, bundleHash);
        }

        return sessionPool.borrowSession(bundleHash);
    }

    public void returnPooledSession(KieSessionPool.PooledKieSession pooledSession) {
        if (!poolConfig.isEnablePooling()) {
            // Pooling disabled, dispose immediately
            pooledSession.dispose();
            return;
        }

        sessionPool.returnSession(pooledSession);
    }

    public void cacheContainer(String bundleHash, KieContainer container) {
        logger.debug("Caching KieContainer for bundleHash: {}", bundleHash);
        containerCache.put(bundleHash, container);
    }

    public KieContainer getCachedContainer(String bundleHash) {
        return containerCache.getIfPresent(bundleHash);
    }

    public boolean isContainerCached(String bundleHash) {
        return containerCache.getIfPresent(bundleHash) != null;
    }

    public void evictContainer(String bundleHash) {
        logger.info("Evicting KieContainer for bundleHash: {}", bundleHash);
        containerCache.invalidate(bundleHash);
        // Also invalidate session pool
        sessionPool.invalidateBundle(bundleHash);
    }

    public void clearCache() {
        logger.info("Clearing all KieContainer cache and session pools");
        containerCache.invalidateAll();
        sessionPool.cleanup();
    }

    public com.github.benmanes.caffeine.cache.stats.CacheStats getCacheStats() {
        return containerCache.stats();
    }

    public long getCacheSize() {
        return containerCache.estimatedSize();
    }

    public KieSessionPool.PoolStatistics getPoolStatistics(String bundleHash) {
        return sessionPool.getStatistics(bundleHash);
    }
}