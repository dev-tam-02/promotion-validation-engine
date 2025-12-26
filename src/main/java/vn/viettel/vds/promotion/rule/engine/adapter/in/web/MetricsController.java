package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.ExecutionMetricsService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.KieSessionManager;

import java.util.HashMap;
import java.util.Map;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/metrics")
@Tag(name = "Rule Execution Metrics", description = "Rule execution metrics and monitoring API")
public class MetricsController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    private static final String METRIC_AVG_EXECUTION_TIME = "averageExecutionTime";

    private final ExecutionMetricsService metricsService;
    private final KieSessionManager sessionManager;

    public MetricsController(ExecutionMetricsService metricsService,
                             KieSessionManager sessionManager) {
        this.metricsService = metricsService;
        this.sessionManager = sessionManager;
    }

    @Operation(summary = "Get execution metrics summary",
            description = "Get overall rule execution metrics summary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    })
    @GetMapping("/summary")
    public ResponseEntity<ExecutionMetricsService.ExecutionSummary> getExecutionSummary() {
        logger.debug("Getting execution metrics summary");

        ExecutionMetricsService.ExecutionSummary summary = metricsService.getExecutionSummary();
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get bundle-specific metrics",
            description = "Get execution metrics for a specific bundle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bundle metrics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Bundle not found")
    })
    @GetMapping("/bundle/{bundleHash}")
    public ResponseEntity<Map<String, Object>> getBundleMetrics(
            @Parameter(description = "Bundle hash") @PathVariable String bundleHash) {

        logger.debug("Getting metrics for bundle: {}", bundleHash);

        long executionCount = metricsService.getBundleExecutionCount(bundleHash);
        long totalExecutionTime = metricsService.getBundleExecutionTime(bundleHash);
        double averageExecutionTime = metricsService.getBundleAverageExecutionTime(bundleHash);
        boolean isCached = sessionManager.isContainerCached(bundleHash);

        Map<String, Object> bundleMetrics = new HashMap<>();
        bundleMetrics.put("bundleHash", bundleHash);
        bundleMetrics.put("executionCount", executionCount);
        bundleMetrics.put("totalExecutionTime", totalExecutionTime);
        bundleMetrics.put(METRIC_AVG_EXECUTION_TIME, averageExecutionTime);
        bundleMetrics.put("cached", isCached);

        if (executionCount == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(bundleMetrics);
    }

    @Operation(summary = "Get all bundle metrics",
            description = "Get execution metrics for all bundles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All bundle metrics retrieved successfully")
    })
    @GetMapping("/bundles")
    public ResponseEntity<Map<String, Object>> getAllBundleMetrics() {
        logger.debug("Getting metrics for all bundles");

        Map<String, Object> allMetrics = new HashMap<>();

        metricsService.getAllBundleExecutionCounts().forEach((bundleHash, count) -> {
            Map<String, Object> bundleMetrics = new HashMap<>();
            bundleMetrics.put("executionCount", count.get());
            bundleMetrics.put("totalExecutionTime", metricsService.getBundleExecutionTime(bundleHash));
            bundleMetrics.put(METRIC_AVG_EXECUTION_TIME, metricsService.getBundleAverageExecutionTime(bundleHash));
            bundleMetrics.put("cached", sessionManager.isContainerCached(bundleHash));

            allMetrics.put(bundleHash, bundleMetrics);
        });

        return ResponseEntity.ok(allMetrics);
    }

    @Operation(summary = "Get cache statistics",
            description = "Get KIE container cache statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache statistics retrieved successfully")
    })
    @GetMapping("/cache")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        logger.debug("Getting cache statistics");

        Map<String, Object> cacheStats = new HashMap<>();
        cacheStats.put("cacheSize", sessionManager.getCacheSize());
        cacheStats.put("cacheStats", sessionManager.getCacheStats());

        return ResponseEntity.ok(cacheStats);
    }

    @Operation(summary = "Clear bundle metrics",
            description = "Clear execution metrics for a specific bundle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bundle metrics cleared successfully"),
            @ApiResponse(responseCode = "404", description = "Bundle not found")
    })
    @DeleteMapping("/bundle/{bundleHash}")
    public ResponseEntity<Void> clearBundleMetrics(
            @Parameter(description = "Bundle hash") @PathVariable String bundleHash) {

        logger.info("Clearing metrics for bundle: {}", bundleHash);

        long executionCount = metricsService.getBundleExecutionCount(bundleHash);
        if (executionCount == 0) {
            return ResponseEntity.notFound().build();
        }

        metricsService.clearBundleMetrics(bundleHash);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Evict bundle from cache",
            description = "Evict a specific bundle from the KIE container cache")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bundle evicted successfully"),
            @ApiResponse(responseCode = "404", description = "Bundle not cached")
    })
    @DeleteMapping("/cache/{bundleHash}")
    public ResponseEntity<Void> evictBundleFromCache(
            @Parameter(description = "Bundle hash") @PathVariable String bundleHash) {

        logger.info("Evicting bundle from cache: {}", bundleHash);

        if (!sessionManager.isContainerCached(bundleHash)) {
            return ResponseEntity.notFound().build();
        }

        sessionManager.evictContainer(bundleHash);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Clear all cache",
            description = "Clear the entire KIE container cache")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache cleared successfully")
    })
    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearAllCache() {
        logger.info("Clearing all cache");

        sessionManager.clearCache();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get health status",
            description = "Get health status of the rule execution engine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Health status retrieved successfully")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        logger.debug("Getting health status");

        ExecutionMetricsService.ExecutionSummary summary = metricsService.getExecutionSummary();

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("totalExecutions", summary.getTotalExecutions());
        health.put("successRate", summary.getSuccessRate());
        health.put(METRIC_AVG_EXECUTION_TIME, summary.getAverageExecutionTime());
        health.put("cacheSize", sessionManager.getCacheSize());
        health.put("timestamp", System.currentTimeMillis());

        // Determine health status based on success rate
        String healthStatus = "UP";
        if (summary.getTotalExecutions() > 0) {
            if (summary.getSuccessRate() < 50.0) {
                healthStatus = "DOWN";
            } else if (summary.getSuccessRate() < 90.0) {
                healthStatus = "DEGRADED";
            }
        }
        health.put("status", healthStatus);

        return ResponseEntity.ok(health);
    }
}