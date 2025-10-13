package vn.viettel.vds.promotion.validation.engine.domain.service.session;

import com.github.benmanes.caffeine.cache.Cache;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.execution.KieSessionCreationException;

@Component
public class KieSessionFactory {

    private static final Logger logger = LoggerFactory.getLogger(KieSessionFactory.class);

    private Cache<String, KieContainer> containerCache;

    public void setContainerCache(Cache<String, KieContainer> containerCache) {
        this.containerCache = containerCache;
    }

    @SuppressWarnings("java:S2095") // Returning session to be managed by caller
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
            String errorMessage = String.format("Failed to create KIE session for bundle: %s", bundleHash);
            logger.error(errorMessage, e);
            throw new KieSessionCreationException(errorMessage, e);
        }
    }

    public boolean canCreateSession(String bundleHash) {
        return containerCache != null && containerCache.getIfPresent(bundleHash) != null;
    }
}