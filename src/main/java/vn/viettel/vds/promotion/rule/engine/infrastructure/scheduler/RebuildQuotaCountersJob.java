package vn.viettel.vds.promotion.rule.engine.infrastructure.scheduler;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.rule.engine.application.port.out.QuotaEventPort;
import vn.viettel.vds.promotion.rule.engine.application.service.QuotaCounterService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scheduled job that rebuilds Redis quota counters from the durable {@code quota_events} backstore.
 *
 * <p>Runs daily at 02:00 UTC by default. Can also be triggered manually via
 * {@code POST /actuator/quota-rebuild} (admin use, no auth in dev profile).
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Query {@code quota_events} to aggregate net count-delta per (ruleId, bucketKey, windowStart)
 *       for all windows still active (windowEnd &ge; now).</li>
 *   <li>For each bucket, call {@link QuotaCounterService#overwrite} — atomically SET the Redis
 *       key to the aggregated value with the correct TTL.</li>
 *   <li>Record the {@code layer1_counter_drift} metric comparing Redis vs DB values.</li>
 * </ol>
 */
@Component
@RestController
@RequestMapping("${spring.application.context-path}/actuator")
public class RebuildQuotaCountersJob {

    private static final Logger log = LoggerFactory.getLogger(RebuildQuotaCountersJob.class);

    private final QuotaEventPort quotaEventPort;
    private final QuotaCounterService quotaCounterService;

    /**
     * Holds the latest computed drift ratio so the Gauge can sample it without
     * triggering a DB query on every scrape.
     */
    private final AtomicReference<Double> lastDriftRatio = new AtomicReference<>(0.0);

    public RebuildQuotaCountersJob(QuotaEventPort quotaEventPort,
                                   QuotaCounterService quotaCounterService,
                                   MeterRegistry meterRegistry) {
        this.quotaEventPort = quotaEventPort;
        this.quotaCounterService = quotaCounterService;

        // Register a Gauge that reports the latest drift computed by the last run
        Gauge.builder("layer1_counter_drift", lastDriftRatio, AtomicReference::get)
                .description("Drift ratio between Layer-1 Redis counters and quota_events DB aggregate. "
                        + "Values > 0.001 (0.1%) indicate divergence requiring attention.")
                .register(meterRegistry);
    }

    /** Daily rebuild at 02:00 UTC. Cron: second minute hour day month weekday. */
    @Scheduled(cron = "0 0 2 * * *")
    public void rebuildScheduled() {
        log.info("RebuildQuotaCountersJob: starting scheduled rebuild");
        runRebuild();
    }

    /** Manual trigger endpoint for ops/admin use. */
    @PostMapping("/quota-rebuild")
    public Map<String, Object> triggerManual() {
        log.info("RebuildQuotaCountersJob: manual trigger via HTTP");
        int rebuilt = runRebuild();
        return Map.of("rebuilt", rebuilt, "driftRatio", lastDriftRatio.get());
    }

    private int runRebuild() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // 1. Aggregate net deltas from DB for active windows
        Map<String, Long> aggregates = quotaEventPort.aggregateActiveWindowCounters(now);
        log.info("RebuildQuotaCountersJob: found {} active window buckets", aggregates.size());

        // 2. SET Redis counters from aggregated values
        int count = 0;
        for (Map.Entry<String, Long> entry : aggregates.entrySet()) {
            try {
                // key format from QuotaEventRepositoryAdapter:
                // "{ruleId}:{bucketKey}:{windowStartEpoch}:wend:{windowEndEpoch}"
                String[] parts = entry.getKey().split(":");
                if (parts.length < 5) {
                    log.warn("RebuildQuotaCountersJob: skipping malformed key={}", entry.getKey());
                    continue;
                }
                String ruleId         = parts[0];
                String bucketKey      = parts[1];
                long windowStartEpoch = Long.parseLong(parts[2]);
                long windowEndEpoch   = Long.parseLong(parts[4]);

                LocalDateTime windowStart = LocalDateTime.ofEpochSecond(windowStartEpoch, 0, ZoneOffset.UTC);
                LocalDateTime windowEnd   = LocalDateTime.ofEpochSecond(windowEndEpoch,   0, ZoneOffset.UTC);

                long netValue = Math.max(0, entry.getValue()); // clamp to 0
                quotaCounterService.overwrite(ruleId, bucketKey, windowStart, windowEnd, netValue);
                count++;
            } catch (RuntimeException e) {
                log.error("RebuildQuotaCountersJob: error rebuilding key={}: {}",
                        entry.getKey(), e.getMessage(), e);
            }
        }

        // 3. Compute and record drift metric
        double drift = quotaCounterService.computeDriftRatio(now);
        lastDriftRatio.set(drift);
        log.info("RebuildQuotaCountersJob: rebuilt={} driftRatio={}", count, drift);
        if (drift > 0.001) {
            log.warn("RebuildQuotaCountersJob: drift={} exceeds 0.1% threshold — counters may be stale", drift);
        }
        return count;
    }
}
