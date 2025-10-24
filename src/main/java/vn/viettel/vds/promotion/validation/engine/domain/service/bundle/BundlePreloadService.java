package vn.viettel.vds.promotion.validation.engine.domain.service.bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.domain.service.DroolsCompilationService;
import vn.viettel.vds.promotion.validation.engine.domain.service.execution.KieSessionManager;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@ConditionalOnProperty(value = "validation.engine.bundle.preload-enabled", havingValue = "true", matchIfMissing = true)
public class BundlePreloadService {

    private static final Logger logger = LoggerFactory.getLogger(BundlePreloadService.class);

    private final BundleRepository bundleRepository;
    private final DroolsCompilationService compilationService;
    private final KieSessionManager sessionManager;
    private final BundlePreloadConfig config;
    private final ExecutorService preloadExecutor;

    public BundlePreloadService(BundleRepository bundleRepository,
                                DroolsCompilationService compilationService,
                                KieSessionManager sessionManager,
                                BundlePreloadConfig config) {
        this.bundleRepository = bundleRepository;
        this.compilationService = compilationService;
        this.sessionManager = sessionManager;
        this.config = config;
        this.preloadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preloadBundlesOnStartup() {
        if (!config.isPreloadOnStartup()) {
            logger.info("Bundle preload on startup is disabled");
            return;
        }

        logger.info("Starting bundle preload on application startup");
        preloadActiveRules();
    }

    public PreloadResult preloadActiveRules() {
        logger.info("Preloading active rule bundles");

        long startTime = System.currentTimeMillis();
        List<ActiveRuleInfo> activeRules = bundleRepository.findActiveRules();

        if (activeRules.isEmpty()) {
            logger.info("No active rules found for preloading");
            return new PreloadResult(0, 0, 0, System.currentTimeMillis() - startTime);
        }

        logger.info("Found {} active rules for preloading", activeRules.size());

        AtomicInteger successful = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = activeRules.stream()
                .map(rule -> CompletableFuture.runAsync(() -> preloadSingleRule(rule, successful, failed, skipped), preloadExecutor))
                .toList();

        // Wait for all preloads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(config.getPreloadTimeoutMinutes(), TimeUnit.MINUTES)
                .join();

        long totalTime = System.currentTimeMillis() - startTime;

        logger.info("Bundle preload completed: successful={}, failed={}, skipped={}, totalTime={}ms",
                successful.get(), failed.get(), skipped.get(), totalTime);

        return new PreloadResult(successful.get(), failed.get(), skipped.get(), totalTime);
    }

    public PreloadResult preloadSpecificRules(List<String> ruleIds) {
        logger.info("Preloading specific rules: {}", ruleIds);

        long startTime = System.currentTimeMillis();
        AtomicInteger successful = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = ruleIds.stream()
                .map(ruleId -> CompletableFuture.runAsync(() -> {
                    ActiveRuleInfo rule = bundleRepository.findActiveRule(ruleId);
                    if (rule != null) {
                        preloadSingleRule(rule, successful, failed, skipped);
                    } else {
                        logger.warn("Rule not found or not active: {}", ruleId);
                        skipped.incrementAndGet();
                    }
                }, preloadExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(config.getPreloadTimeoutMinutes(), TimeUnit.MINUTES)
                .join();

        long totalTime = System.currentTimeMillis() - startTime;

        logger.info("Specific rule preload completed: successful={}, failed={}, skipped={}, totalTime={}ms",
                successful.get(), failed.get(), skipped.get(), totalTime);

        return new PreloadResult(successful.get(), failed.get(), skipped.get(), totalTime);
    }

    private void preloadSingleRule(ActiveRuleInfo rule, AtomicInteger successful, AtomicInteger failed, AtomicInteger skipped) {
        try {
            logger.debug("Preloading rule: ruleId={}, bundleHash={}", rule.getRuleId(), rule.getBundleHash());

            // Check if already cached
            if (sessionManager.isContainerCached(rule.getBundleHash())) {
                logger.debug("Rule already cached, skipping: {}", rule.getRuleId());
                skipped.incrementAndGet();
                return;
            }

            // Create KIE container from bundle
            if (rule.getCompiledBytes() != null) {
                org.kie.api.runtime.KieContainer container = compilationService.createKieContainer(rule.getCompiledBytes());
                sessionManager.cacheContainer(rule.getBundleHash(), container);

                // Optionally pre-warm session pool
                if (config.isPrewarmSessionPool()) {
                    prewarmSessionPool(rule.getBundleHash());
                }

                logger.debug("Successfully preloaded rule: {}", rule.getRuleId());
                successful.incrementAndGet();

            } else {
                logger.warn("No compiled bytes available for rule: {}", rule.getRuleId());
                skipped.incrementAndGet();
            }

        } catch (Exception e) {
            logger.error("Failed to preload rule: ruleId={}", rule.getRuleId(), e);
            failed.incrementAndGet();
        }
    }

    private void prewarmSessionPool(String bundleHash) {
        try {
            // Create a few sessions to warm up the pool
            for (int i = 0; i < config.getPrewarmSessionCount(); i++) {
                org.kie.api.runtime.KieContainer container = sessionManager.getCachedContainer(bundleHash);
                if (container != null) {
                    try (org.kie.api.runtime.KieSession session = container.newKieSession()) {
                        // Session is created and automatically disposed, nothing needed here.
                    }
                }
            }
            logger.debug("Session pool prewarmed for bundle: {}", bundleHash);

        } catch (Exception e) {
            logger.warn("Failed to prewarm session pool for bundle: {}", bundleHash, e);
        }
    }

    public void evictBundle(String bundleHash) {
        logger.info("Evicting bundle from cache: {}", bundleHash);
        sessionManager.evictContainer(bundleHash);
    }

    private boolean testContainerHealth(ActiveRuleInfo rule) {
        try {
            org.kie.api.runtime.KieContainer container = sessionManager.getCachedContainer(rule.getBundleHash());
            if (container != null && compilationService.validateArtifact(rule.getCompiledBytes())) {
                return testSessionCreation(container, rule.getRuleId());
            }
        } catch (Exception e) {
            logger.warn("Bundle health check failed for rule: {}", rule.getRuleId(), e);
        }
        return false; // Not healthy
    }

    private boolean testSessionCreation(org.kie.api.runtime.KieContainer container, String ruleId) {
        try (org.kie.api.runtime.KieSession session = container.newKieSession()) {
            return true; // Healthy
        } catch (Exception e) {
            logger.warn("Error creating or disposing KieSession during health check for rule: {}", ruleId, e);
            return false;
        }
    }

    @SuppressWarnings("java:S3776")
    // Suppressing cognitive complexity as the nested logic is required for proper health checking
    public BundleHealthStatus checkBundleHealth() {
        logger.debug("Checking bundle health");

        long startTime = System.currentTimeMillis();
        List<ActiveRuleInfo> activeRules = bundleRepository.findActiveRules();

        int totalRules = activeRules.size();
        int loadedRules = 0;
        int healthyRules = 0;

        for (ActiveRuleInfo rule : activeRules) {
            if (sessionManager.isContainerCached(rule.getBundleHash())) {
                loadedRules++;

                if (testContainerHealth(rule)) {
                    healthyRules++;
                }
            }
        }

        long checkTime = System.currentTimeMillis() - startTime;

        BundleHealthStatus status = new BundleHealthStatus(
                totalRules, loadedRules, healthyRules, checkTime, Instant.now()
        );

        logger.debug("Bundle health check completed: total={}, loaded={}, healthy={}, time={}ms",
                totalRules, loadedRules, healthyRules, checkTime);

        return status;
    }

    // Result classes
    public static class PreloadResult {
        private final int successful;
        private final int failed;
        private final int skipped;
        private final long totalTimeMs;

        public PreloadResult(int successful, int failed, int skipped, long totalTimeMs) {
            this.successful = successful;
            this.failed = failed;
            this.skipped = skipped;
            this.totalTimeMs = totalTimeMs;
        }

        public int getSuccessful() {
            return successful;
        }

        public int getFailed() {
            return failed;
        }

        public int getSkipped() {
            return skipped;
        }

        public long getTotalTimeMs() {
            return totalTimeMs;
        }

        public int getTotal() {
            return successful + failed + skipped;
        }

        public double getSuccessRate() {
            int total = getTotal();
            return total > 0 ? (double) successful / total : 0.0;
        }
    }

    public static class BundleHealthStatus {
        private final int totalRules;
        private final int loadedRules;
        private final int healthyRules;
        private final long checkTimeMs;
        private final Instant checkedAt;

        public BundleHealthStatus(int totalRules, int loadedRules, int healthyRules,
                                  long checkTimeMs, Instant checkedAt) {
            this.totalRules = totalRules;
            this.loadedRules = loadedRules;
            this.healthyRules = healthyRules;
            this.checkTimeMs = checkTimeMs;
            this.checkedAt = checkedAt;
        }

        public int getTotalRules() {
            return totalRules;
        }

        public int getLoadedRules() {
            return loadedRules;
        }

        public int getHealthyRules() {
            return healthyRules;
        }

        public long getCheckTimeMs() {
            return checkTimeMs;
        }

        public Instant getCheckedAt() {
            return checkedAt;
        }

        public double getLoadedRatio() {
            return totalRules > 0 ? (double) loadedRules / totalRules : 0.0;
        }

        public double getHealthyRatio() {
            return loadedRules > 0 ? (double) healthyRules / loadedRules : 0.0;
        }
    }
}