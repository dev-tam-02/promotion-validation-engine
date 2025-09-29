package vn.viettel.vds.promotion.validation.engine.domain.service.session;

import com.github.benmanes.caffeine.cache.Cache;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KieSessionFactory {

    private static final Logger logger = LoggerFactory.getLogger(KieSessionFactory.class);

    private Cache<String, KieContainer> containerCache;

    public void setContainerCache(Cache<String, KieContainer> containerCache) {
        this.containerCache = containerCache;
    }

    public KieSession createSession(String bundleHash) {
        logger.debug("Creating new KIE session for bundle: {}", bundleHash);

        if (containerCache == null) {
            throw new IllegalStateException("Container cache not initialized");
        }

        KieContainer container = containerCache.getIfPresent(bundleHash);
        if (container == null) {
            throw new IllegalArgumentException("No cached container found for bundle: " + bundleHash);
        }

        try {
            KieSession session = container.newKieSession();
            logger.debug("Successfully created KIE session for bundle: {}", bundleHash);
            return session;

        } catch (Exception e) {
            logger.error("Failed to create KIE session for bundle: {}", bundleHash, e);
            throw new RuntimeException("Failed to create KIE session: " + e.getMessage(), e);
        }
    }

    public boolean canCreateSession(String bundleHash) {
        return containerCache != null && containerCache.getIfPresent(bundleHash) != null;
    }
}