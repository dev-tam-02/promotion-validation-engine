package vn.viettel.vds.promotion.rule.engine.adapter.out.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Micrometer metrics for per-rule analytics observability (Task 09 V10).
 *
 * <p>Three metric families:
 * <ul>
 *   <li>{@code rule_fires_total{rule_id, verdict}} — Counter: one increment per rule evaluation,
 *       tagged with the final verdict (ALLOW | DENY).</li>
 *   <li>{@code rule_rejects_total{rule_id, reason_code}} — Counter: one increment per reason code
 *       when a rule evaluation produces verdict=DENY.</li>
 *   <li>{@code rule_evaluation_duration_seconds{rule_id}} — Timer histogram: wraps each
 *       per-rule execution in SimulateEvaluationService. Exposes Prometheus histogram buckets
 *       so Grafana can compute {@code histogram_quantile(0.95, ...)}.</li>
 * </ul>
 *
 * <p>Usage pattern (called from SimulateEvaluationService.evaluate loop):
 * <pre>
 *   Timer.Sample sample = metrics.startEvaluationTimer();
 *   SingleEvalResult result = executeSingle(rule, facts, simulateMode);
 *   metrics.stopEvaluationTimer(sample, ruleId);
 *   metrics.recordFire(ruleId, result.verdict());
 *   if ("DENY".equals(result.verdict())) {
 *       metrics.recordRejects(ruleId, result.reasonCodes());
 *   }
 * </pre>
 */
@Component
public class RuleAnalyticsMetrics {

    private static final String METRIC_FIRES = "rule_fires_total";
    private static final String METRIC_REJECTS = "rule_rejects_total";
    private static final String METRIC_DURATION = "rule_evaluation_duration_seconds";

    private static final String TAG_RULE_ID = "rule_id";
    private static final String TAG_VERDICT = "verdict";
    private static final String TAG_REASON_CODE = "reason_code";

    private final MeterRegistry meterRegistry;

    public RuleAnalyticsMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Start a timer sample for a rule evaluation. Call {@link #stopEvaluationTimer} after
     * the rule execution completes to record the observation.
     */
    public Timer.Sample startEvaluationTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop the timer and record the observation to the histogram for the given rule.
     *
     * @param sample the sample returned by {@link #startEvaluationTimer()}
     * @param ruleId the rule being evaluated
     */
    public void stopEvaluationTimer(Timer.Sample sample, String ruleId) {
        Timer timer = Timer.builder(METRIC_DURATION)
                .description("Per-rule evaluation duration (histogram for p95 queries)")
                .tag(TAG_RULE_ID, sanitize(ruleId))
                .publishPercentileHistogram(true)
                .register(meterRegistry);
        sample.stop(timer);
    }

    /**
     * Increment {@code rule_fires_total} for the given rule + verdict.
     *
     * @param ruleId  rule identifier
     * @param verdict "ALLOW" or "DENY"
     */
    public void recordFire(String ruleId, String verdict) {
        Counter.builder(METRIC_FIRES)
                .description("Total rule evaluation fires by rule_id and verdict")
                .tag(TAG_RULE_ID, sanitize(ruleId))
                .tag(TAG_VERDICT, verdict != null ? verdict : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Increment {@code rule_rejects_total} for each reason code in a DENY verdict.
     *
     * @param ruleId      rule identifier
     * @param reasonCodes list of reason codes from the evaluation
     */
    public void recordRejects(String ruleId, List<String> reasonCodes) {
        if (reasonCodes == null || reasonCodes.isEmpty()) {
            // Record a generic DENY_NO_REASON so dead-rule queries still see this rule
            Counter.builder(METRIC_REJECTS)
                    .description("Total rule rejections by rule_id and reason_code")
                    .tag(TAG_RULE_ID, sanitize(ruleId))
                    .tag(TAG_REASON_CODE, "DENY_NO_REASON")
                    .register(meterRegistry)
                    .increment();
            return;
        }
        for (String code : reasonCodes) {
            Counter.builder(METRIC_REJECTS)
                    .description("Total rule rejections by rule_id and reason_code")
                    .tag(TAG_RULE_ID, sanitize(ruleId))
                    .tag(TAG_REASON_CODE, sanitize(code))
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * Sanitize a tag value for Prometheus: replace characters that are illegal in label values
     * with underscores, and truncate to 128 characters.
     */
    private String sanitize(String value) {
        if (value == null) {
            return "null";
        }
        String safe = value.replaceAll("[^a-zA-Z0-9_:\\-./]", "_");
        return safe.length() > 128 ? safe.substring(0, 128) : safe;
    }
}
