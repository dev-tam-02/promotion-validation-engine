package vn.viettel.vds.promotion.rule.engine.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.engine.event.BundleCacheInvalidationEvent;
import vn.viettel.vds.promotion.rule.engine.adapter.in.messaging.config.BroadcastKafkaConfig;
import vn.viettel.vds.promotion.rule.engine.domain.service.bundle.BundlePreloadService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.KieSessionManager;

import java.util.List;

/**
 * Kafka listener for bundle cache invalidation events.
 * Uses broadcast pattern (unique group-id per instance) to ensure
 * all validation-engine instances receive invalidation notifications.
 */
@Component
public class BundleCacheInvalidationListener {

    private static final Logger logger = LoggerFactory.getLogger(BundleCacheInvalidationListener.class);

    private final KieSessionManager kieSessionManager;
    private final BundlePreloadService bundlePreloadService;
    private final BroadcastKafkaConfig broadcastKafkaConfig;

    public BundleCacheInvalidationListener(KieSessionManager kieSessionManager,
                                           BundlePreloadService bundlePreloadService,
                                           BroadcastKafkaConfig broadcastKafkaConfig) {
        this.kieSessionManager = kieSessionManager;
        this.bundlePreloadService = bundlePreloadService;
        this.broadcastKafkaConfig = broadcastKafkaConfig;
    }

    /**
     * Listen for cache invalidation events from Kafka.
     * Uses broadcastListenerContainerFactory which has unique group-id per instance.
     */
    @KafkaListener(
            topics = "${kafka.topics.bundle-cache-invalidation:promotion_bundle_cache_invalidation}",
            containerFactory = "broadcastListenerContainerFactory"
    )
    public void onCacheInvalidation(BundleCacheInvalidationEvent event) {
        String currentInstanceId = broadcastKafkaConfig.getInstanceId();

        logger.info("Received cache invalidation event: eventId={}, bundleHash={}, type={}, sourceInstance={}",
                event.getEventId(), event.getBundleHash(), event.getType(), event.getSourceInstanceId());

        // Skip if this instance is the source (already processed locally)
        if (currentInstanceId.equals(event.getSourceInstanceId())) {
            logger.debug("Skipping cache invalidation from self: instanceId={}", currentInstanceId);
            return;
        }

        try {
            handleInvalidation(event);
            logger.info("Successfully processed cache invalidation: eventId={}, instanceId={}",
                    event.getEventId(), currentInstanceId);

        } catch (Exception e) {
            logger.error("Failed to process cache invalidation: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
        }
    }

    private void handleInvalidation(BundleCacheInvalidationEvent event) {
        switch (event.getType()) {
            case BUNDLE_UPDATED -> handleBundleUpdated(event);
            case BUNDLE_DELETED -> handleBundleDeleted(event);
            case FULL_INVALIDATION -> handleFullInvalidation();
            default -> logger.warn("Unknown invalidation type: {}", event.getType());
        }
    }

    /**
     * Handle bundle updated: evict old cache and preload new bundle.
     */
    private void handleBundleUpdated(BundleCacheInvalidationEvent event) {
        String bundleHash = event.getBundleHash();
        String ruleId = event.getRuleId();

        logger.info("Handling BUNDLE_UPDATED: bundleHash={}, ruleId={}", bundleHash, ruleId);

        // Evict the old cached container
        kieSessionManager.evictContainer(bundleHash);

        // Preload the new bundle from MinIO
        if (ruleId != null) {
            try {
                BundlePreloadService.PreloadResult result = bundlePreloadService.preloadSpecificRules(List.of(ruleId));
                logger.info("Preload result for ruleId={}: successful={}, failed={}, skipped={}",
                        ruleId, result.getSuccessful(), result.getFailed(), result.getSkipped());
            } catch (Exception e) {
                logger.warn("Failed to preload rule after cache invalidation: ruleId={}", ruleId, e);
            }
        }
    }

    /**
     * Handle bundle deleted: evict cache without reload.
     */
    private void handleBundleDeleted(BundleCacheInvalidationEvent event) {
        String bundleHash = event.getBundleHash();

        logger.info("Handling BUNDLE_DELETED: bundleHash={}", bundleHash);

        // Just evict, no preload needed
        kieSessionManager.evictContainer(bundleHash);
    }

    /**
     * Handle full invalidation: clear all caches.
     */
    private void handleFullInvalidation() {
        logger.info("Handling FULL_INVALIDATION: clearing all caches");

        kieSessionManager.clearCache();

        // Preload all active rules
        bundlePreloadService.preloadActiveRules();
    }
}
