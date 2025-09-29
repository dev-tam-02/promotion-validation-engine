package vn.viettel.vds.promotion.validation.engine.domain.service.execution;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ExecutionMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionMetricsService.class);

    private final MeterRegistry meterRegistry;

    // Metrics counters
    private final Counter totalExecutionsCounter;
    private final Counter successfulExecutionsCounter;
    private final Counter failedExecutionsCounter;
    private final Counter batchExecutionsCounter;

    // Timing metrics
    private final Timer executionTimer;
    private final Timer compilationTimer;

    // Custom metrics storage
    private final ConcurrentMap<String, AtomicLong> bundleExecutionCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> bundleExecutionTimes = new ConcurrentHashMap<>();

    public ExecutionMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.totalExecutionsCounter = Counter.builder("drools.executions.total")
            .description("Total number of rule executions")
            .register(meterRegistry);

        this.successfulExecutionsCounter = Counter.builder("drools.executions.successful")
            .description("Number of successful rule executions")
            .register(meterRegistry);

        this.failedExecutionsCounter = Counter.builder("drools.executions.failed")
            .description("Number of failed rule executions")
            .register(meterRegistry);

        this.batchExecutionsCounter = Counter.builder("drools.executions.batch")
            .description("Number of batch rule executions")
            .register(meterRegistry);

        // Initialize timers
        this.executionTimer = Timer.builder("drools.execution.duration")
            .description("Rule execution duration")
            .register(meterRegistry);

        this.compilationTimer = Timer.builder("drools.compilation.duration")
            .description("Rule compilation duration")
            .register(meterRegistry);
    }

    public void recordExecution(String bundleHash, Duration duration, boolean success) {
        totalExecutionsCounter.increment();

        if (success) {
            successfulExecutionsCounter.increment();
        } else {
            failedExecutionsCounter.increment();
        }

        executionTimer.record(duration);

        // Record bundle-specific metrics
        bundleExecutionCounts.computeIfAbsent(bundleHash, k -> new AtomicLong(0)).incrementAndGet();
        bundleExecutionTimes.computeIfAbsent(bundleHash, k -> new AtomicLong(0))
            .addAndGet(duration.toMillis());

        logger.debug("Recorded execution metrics: bundle={}, duration={}ms, success={}",
                    bundleHash, duration.toMillis(), success);
    }

    public void recordBatchExecution(int batchSize, Duration totalDuration, int successCount) {
        batchExecutionsCounter.increment();

        // Record individual metrics for the batch
        for (int i = 0; i < successCount; i++) {
            successfulExecutionsCounter.increment();
        }

        int failedCount = batchSize - successCount;
        for (int i = 0; i < failedCount; i++) {
            failedExecutionsCounter.increment();
        }

        totalExecutionsCounter.increment(batchSize);
        executionTimer.record(totalDuration);

        logger.info("Recorded batch execution metrics: size={}, duration={}ms, success={}, failed={}",
                   batchSize, totalDuration.toMillis(), successCount, failedCount);
    }

    public void recordCompilation(Duration duration, boolean success) {
        compilationTimer.record(duration);

        if (success) {
            meterRegistry.counter("drools.compilations.successful").increment();
        } else {
            meterRegistry.counter("drools.compilations.failed").increment();
        }

        logger.debug("Recorded compilation metrics: duration={}ms, success={}",
                    duration.toMillis(), success);
    }

    public void recordCacheHit(String bundleHash) {
        meterRegistry.counter("drools.cache.hits", "bundle", bundleHash).increment();
    }

    public void recordCacheMiss(String bundleHash) {
        meterRegistry.counter("drools.cache.misses", "bundle", bundleHash).increment();
    }

    public void recordSessionCreation(String sessionType) {
        meterRegistry.counter("drools.sessions.created", "type", sessionType).increment();
    }

    public void recordRuleFired(String ruleName, String bundleHash) {
        meterRegistry.counter("drools.rules.fired", "rule", ruleName, "bundle", bundleHash).increment();
    }

    // Getter methods for metrics
    public long getTotalExecutions() {
        return (long) totalExecutionsCounter.count();
    }

    public long getSuccessfulExecutions() {
        return (long) successfulExecutionsCounter.count();
    }

    public long getFailedExecutions() {
        return (long) failedExecutionsCounter.count();
    }

    public long getBatchExecutions() {
        return (long) batchExecutionsCounter.count();
    }

    public double getAverageExecutionTime() {
        return executionTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public double getMaxExecutionTime() {
        return executionTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public long getBundleExecutionCount(String bundleHash) {
        AtomicLong count = bundleExecutionCounts.get(bundleHash);
        return count != null ? count.get() : 0;
    }

    public long getBundleExecutionTime(String bundleHash) {
        AtomicLong time = bundleExecutionTimes.get(bundleHash);
        return time != null ? time.get() : 0;
    }

    public double getBundleAverageExecutionTime(String bundleHash) {
        long count = getBundleExecutionCount(bundleHash);
        long totalTime = getBundleExecutionTime(bundleHash);
        return count > 0 ? (double) totalTime / count : 0.0;
    }

    public ConcurrentMap<String, AtomicLong> getAllBundleExecutionCounts() {
        return new ConcurrentHashMap<>(bundleExecutionCounts);
    }

    public void clearBundleMetrics(String bundleHash) {
        bundleExecutionCounts.remove(bundleHash);
        bundleExecutionTimes.remove(bundleHash);
        logger.info("Cleared metrics for bundle: {}", bundleHash);
    }

    public ExecutionSummary getExecutionSummary() {
        return new ExecutionSummary(
            getTotalExecutions(),
            getSuccessfulExecutions(),
            getFailedExecutions(),
            getBatchExecutions(),
            getAverageExecutionTime(),
            getMaxExecutionTime()
        );
    }

    public ExecutionStats getExecutionStats() {
        Map<String, Object> bundleStats = new HashMap<>();
        for (Map.Entry<String, AtomicLong> entry : bundleExecutionCounts.entrySet()) {
            String bundleHash = entry.getKey();
            bundleStats.put(bundleHash, Map.of(
                "executionCount", entry.getValue().get(),
                "totalTime", getBundleExecutionTime(bundleHash),
                "averageTime", getBundleAverageExecutionTime(bundleHash)
            ));
        }

        return new ExecutionStats(
            getTotalExecutions(),
            getSuccessfulExecutions(),
            getFailedExecutions(),
            getBatchExecutions(),
            getAverageExecutionTime(),
            getMaxExecutionTime(),
            bundleStats,
            Instant.now()
        );
    }

    public static class ExecutionSummary {
        private final long totalExecutions;
        private final long successfulExecutions;
        private final long failedExecutions;
        private final long batchExecutions;
        private final double averageExecutionTime;
        private final double maxExecutionTime;

        public ExecutionSummary(long totalExecutions, long successfulExecutions, long failedExecutions,
                               long batchExecutions, double averageExecutionTime, double maxExecutionTime) {
            this.totalExecutions = totalExecutions;
            this.successfulExecutions = successfulExecutions;
            this.failedExecutions = failedExecutions;
            this.batchExecutions = batchExecutions;
            this.averageExecutionTime = averageExecutionTime;
            this.maxExecutionTime = maxExecutionTime;
        }

        // Getters
        public long getTotalExecutions() { return totalExecutions; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
        public long getBatchExecutions() { return batchExecutions; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public double getMaxExecutionTime() { return maxExecutionTime; }
        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions * 100 : 0.0;
        }
    }

    public static class ExecutionStats {
        private final long totalExecutions;
        private final long successfulExecutions;
        private final long failedExecutions;
        private final long batchExecutions;
        private final double averageExecutionTime;
        private final double maxExecutionTime;
        private final Map<String, Object> bundleStats;
        private final Instant timestamp;

        public ExecutionStats(long totalExecutions, long successfulExecutions, long failedExecutions,
                             long batchExecutions, double averageExecutionTime, double maxExecutionTime,
                             Map<String, Object> bundleStats, Instant timestamp) {
            this.totalExecutions = totalExecutions;
            this.successfulExecutions = successfulExecutions;
            this.failedExecutions = failedExecutions;
            this.batchExecutions = batchExecutions;
            this.averageExecutionTime = averageExecutionTime;
            this.maxExecutionTime = maxExecutionTime;
            this.bundleStats = bundleStats;
            this.timestamp = timestamp;
        }

        // Getters
        public long getTotalExecutions() { return totalExecutions; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
        public long getBatchExecutions() { return batchExecutions; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public double getMaxExecutionTime() { return maxExecutionTime; }
        public Map<String, Object> getBundleStats() { return bundleStats; }
        public Instant getTimestamp() { return timestamp; }
        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions * 100 : 0.0;
        }
    }
}