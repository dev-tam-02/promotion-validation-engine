package vn.viettel.vds.promotion.validation.engine.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.session.KieSessionPool;
import vn.viettel.vds.promotion.validation.engine.domain.service.session.SessionPoolConfig;

@Component
@ConditionalOnProperty(value = "validation.engine.session-pool.enable-pooling", havingValue = "true", matchIfMissing = true)
public class SessionPoolCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SessionPoolCleanupScheduler.class);

    private final KieSessionPool sessionPool;
    private final SessionPoolConfig config;

    public SessionPoolCleanupScheduler(KieSessionPool sessionPool, SessionPoolConfig config) {
        this.sessionPool = sessionPool;
        this.config = config;
    }

    @Scheduled(fixedRateString = "#{${validation.engine.session-pool.cleanup-interval-minutes:15} * 60 * 1000}")
    public void cleanupExpiredSessions() {
        logger.debug("Starting scheduled session pool cleanup");

        try {
            sessionPool.cleanup();
            logger.debug("Session pool cleanup completed successfully");

        } catch (Exception e) {
            logger.error("Error during session pool cleanup", e);
        }
    }
}