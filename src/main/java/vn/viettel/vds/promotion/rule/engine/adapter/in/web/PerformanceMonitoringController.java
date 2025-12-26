package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.rule.engine.domain.service.bundle.BundlePreloadService;
import vn.viettel.vds.promotion.rule.engine.domain.service.compilation.CachedCompilationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.compilation.CompilationCacheService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.ExecutionMetricsService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.KieSessionManager;
import vn.viettel.vds.promotion.rule.engine.domain.service.session.KieSessionPool;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/api/v1/performance")
@Tag(name = "Performance Monitoring", description = "Performance monitoring and cache management APIs")
@ConditionalOnProperty(value = "validation.engine.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceMonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringController.class);

    private final KieSessionManager sessionManager;
    private final CachedCompilationService compilationService;
    private final BundlePreloadService preloadService;
    private final ExecutionMetricsService metricsService;

    public PerformanceMonitoringController(KieSessionManager sessionManager,
                                           CachedCompilationService compilationService,
                                           BundlePreloadService preloadService,
                                           ExecutionMetricsService metricsService) {
        this.sessionManager = sessionManager;
        this.compilationService = compilationService;
        this.preloadService = preloadService;
        this.metricsService = metricsService;
    }

    @Operation(summary = "Get performance overview",
            description = "Get comprehensive performance metrics overview")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Performance metrics retrieved successfully")
    })
    @GetMapping("/overview")
    public ResponseEntity<PerformanceOverview> getPerformanceOverview() {
        logger.debug("Getting performance overview");

        try {
            // Session pool stats
            // Note: This would need session pool statistics implementation
            Map<String, Object> sessionStats = new HashMap<>();
            sessionStats.put("cacheSize", sessionManager.getCacheSize());

            // Compilation cache stats
            CompilationCacheService.CompilationCacheStats compilationStats = compilationService.getCacheStats();

            // Bundle health
            BundlePreloadService.BundleHealthStatus bundleHealth = preloadService.checkBundleHealth();

            // Execution metrics
            ExecutionMetricsService.ExecutionStats executionStats = metricsService.getExecutionStats();

            PerformanceOverview overview = new PerformanceOverview(
                    sessionStats,
                    compilationStats,
                    bundleHealth,
                    executionStats,
                    Instant.now()
            );

            return ResponseEntity.ok(overview);

        } catch (Exception e) {
            logger.error("Failed to get performance overview", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get session pool statistics",
            description = "Get detailed statistics for KIE session pools")
    @GetMapping("/session-pools")
    public ResponseEntity<Map<String, KieSessionPool.PoolStatistics>> getSessionPoolStats() {
        logger.debug("Getting session pool statistics");

        try {
            // This would need implementation in sessionManager to get pool stats for all bundles
            Map<String, KieSessionPool.PoolStatistics> stats = new HashMap<>();
            // Future enhancement: Implement getting stats for all bundles from session manager
            // This would involve querying the KieSessionManager for bundle statistics

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Failed to get session pool statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get session pool statistics for specific bundle",
            description = "Get detailed statistics for a specific bundle's session pool")
    @GetMapping("/session-pools/{bundleHash}")
    public ResponseEntity<KieSessionPool.PoolStatistics> getSessionPoolStats(
            @Parameter(description = "Bundle hash") @PathVariable String bundleHash) {

        logger.debug("Getting session pool statistics for bundle: {}", bundleHash);

        try {
            KieSessionPool.PoolStatistics stats = sessionManager.getPoolStatistics(bundleHash);

            if (stats != null) {
                return ResponseEntity.ok(stats);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Failed to get session pool statistics for bundle: {}", bundleHash, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get compilation cache statistics",
            description = "Get detailed compilation cache performance metrics")
    @GetMapping("/compilation-cache")
    public ResponseEntity<CompilationCacheService.CompilationCacheStats> getCompilationCacheStats() {
        logger.debug("Getting compilation cache statistics");

        try {
            CompilationCacheService.CompilationCacheStats stats = compilationService.getCacheStats();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Failed to get compilation cache statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Clear compilation cache",
            description = "Clear all entries from the compilation cache")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache cleared successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to clear cache")
    })
    @PostMapping("/compilation-cache/clear")
    public ResponseEntity<Map<String, String>> clearCompilationCache() {
        logger.info("Clearing compilation cache");

        try {
            compilationService.clearCache();

            Map<String, String> response = new HashMap<>();
            response.put("message", "Compilation cache cleared successfully");
            response.put("timestamp", Instant.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to clear compilation cache", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get bundle health status",
            description = "Get health status of all active rule bundles")
    @GetMapping("/bundle-health")
    public ResponseEntity<BundlePreloadService.BundleHealthStatus> getBundleHealth() {
        logger.debug("Getting bundle health status");

        try {
            BundlePreloadService.BundleHealthStatus health = preloadService.checkBundleHealth();
            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("Failed to get bundle health status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Preload active rules",
            description = "Manually trigger preloading of all active rules")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preload completed"),
            @ApiResponse(responseCode = "500", description = "Preload failed")
    })
    @PostMapping("/preload/active")
    public ResponseEntity<BundlePreloadService.PreloadResult> preloadActiveRules() {
        logger.info("Manual preload of active rules requested");

        try {
            BundlePreloadService.PreloadResult result = preloadService.preloadActiveRules();
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to preload active rules", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Preload specific rules",
            description = "Manually trigger preloading of specific rules")
    @PostMapping("/preload/specific")
    public ResponseEntity<BundlePreloadService.PreloadResult> preloadSpecificRules(
            @RequestBody List<String> ruleIds) {

        logger.info("Manual preload of specific rules requested: {}", ruleIds);

        try {
            BundlePreloadService.PreloadResult result = preloadService.preloadSpecificRules(ruleIds);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to preload specific rules: {}", ruleIds, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get execution metrics",
            description = "Get detailed rule execution performance metrics")
    @GetMapping("/execution-metrics")
    public ResponseEntity<ExecutionMetricsService.ExecutionStats> getExecutionMetrics() {
        logger.debug("Getting execution metrics");

        try {
            ExecutionMetricsService.ExecutionStats stats = metricsService.getExecutionStats();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Failed to get execution metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Response classes
    public static class PerformanceOverview {
        private final Map<String, Object> sessionPoolStats;
        private final CompilationCacheService.CompilationCacheStats compilationCacheStats;
        private final BundlePreloadService.BundleHealthStatus bundleHealth;
        private final ExecutionMetricsService.ExecutionStats executionStats;
        private final Instant timestamp;

        public PerformanceOverview(Map<String, Object> sessionPoolStats,
                                   CompilationCacheService.CompilationCacheStats compilationCacheStats,
                                   BundlePreloadService.BundleHealthStatus bundleHealth,
                                   ExecutionMetricsService.ExecutionStats executionStats,
                                   Instant timestamp) {
            this.sessionPoolStats = sessionPoolStats;
            this.compilationCacheStats = compilationCacheStats;
            this.bundleHealth = bundleHealth;
            this.executionStats = executionStats;
            this.timestamp = timestamp;
        }

        // Getters
        public Map<String, Object> getSessionPoolStats() {
            return sessionPoolStats;
        }

        public CompilationCacheService.CompilationCacheStats getCompilationCacheStats() {
            return compilationCacheStats;
        }

        public BundlePreloadService.BundleHealthStatus getBundleHealth() {
            return bundleHealth;
        }

        public ExecutionMetricsService.ExecutionStats getExecutionStats() {
            return executionStats;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }
}