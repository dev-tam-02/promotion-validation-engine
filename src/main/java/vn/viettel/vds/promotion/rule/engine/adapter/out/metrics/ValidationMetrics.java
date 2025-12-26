package vn.viettel.vds.promotion.rule.engine.adapter.out.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class ValidationMetrics {

    private final Counter validationRequestsTotal;
    private final Counter validationSuccessTotal;
    private final Counter validationFailureTotal;
    private final Counter ruleBundleCacheHits;
    private final Counter ruleBundleCacheMisses;
    private final Counter limitsSnapshotCacheHits;
    private final Counter limitsSnapshotCacheMisses;
    private final Timer validationLatency;
    private final Timer ruleExecutionLatency;

    public ValidationMetrics(MeterRegistry meterRegistry) {
        this.validationRequestsTotal = Counter.builder("validation_requests_total")
                .description("Total number of validation requests")
                .register(meterRegistry);

        this.validationSuccessTotal = Counter.builder("validation_success_total")
                .description("Total number of successful validations")
                .tag("result", "valid")
                .register(meterRegistry);

        this.validationFailureTotal = Counter.builder("validation_failure_total")
                .description("Total number of failed validations")
                .tag("result", "invalid")
                .register(meterRegistry);

        this.ruleBundleCacheHits = Counter.builder("rule_bundle_cache_hits_total")
                .description("Total rule bundle cache hits")
                .register(meterRegistry);

        this.ruleBundleCacheMisses = Counter.builder("rule_bundle_cache_misses_total")
                .description("Total rule bundle cache misses")
                .register(meterRegistry);

        this.limitsSnapshotCacheHits = Counter.builder("limits_snapshot_cache_hits_total")
                .description("Total limits snapshot cache hits")
                .register(meterRegistry);

        this.limitsSnapshotCacheMisses = Counter.builder("limits_snapshot_cache_misses_total")
                .description("Total limits snapshot cache misses")
                .register(meterRegistry);

        this.validationLatency = Timer.builder("validation_latency")
                .description("Validation request latency")
                .register(meterRegistry);

        this.ruleExecutionLatency = Timer.builder("rule_execution_latency")
                .description("Rule execution latency")
                .register(meterRegistry);
    }

    public void incrementValidationRequests() {
        validationRequestsTotal.increment();
    }

    public void incrementValidationSuccess() {
        validationSuccessTotal.increment();
    }

    public void incrementValidationFailure() {
        validationFailureTotal.increment();
    }

    public void incrementRuleBundleCacheHit() {
        ruleBundleCacheHits.increment();
    }

    public void incrementRuleBundleCacheMiss() {
        ruleBundleCacheMisses.increment();
    }

    public void incrementLimitsSnapshotCacheHit() {
        limitsSnapshotCacheHits.increment();
    }

    public void incrementLimitsSnapshotCacheMiss() {
        limitsSnapshotCacheMisses.increment();
    }

    public Timer.Sample startValidationTimer() {
        return Timer.start();
    }

    public void recordValidationLatency(Timer.Sample sample) {
        sample.stop(validationLatency);
    }

    public Timer.Sample startRuleExecutionTimer() {
        return Timer.start();
    }

    public void recordRuleExecutionLatency(Timer.Sample sample) {
        sample.stop(ruleExecutionLatency);
    }

    public void recordValidationLatency(Duration duration) {
        validationLatency.record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }
}