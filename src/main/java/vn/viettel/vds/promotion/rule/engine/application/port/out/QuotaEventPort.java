package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.QuotaEventEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Outbound port for persisting and querying quota-event records.
 */
public interface QuotaEventPort {

    /**
     * Persist a single quota event (INCR, DECR, or ROLLBACK).
     */
    void save(QuotaEventEntity event);

    /**
     * Find all uncompensated INCR/DECR events for a redemption.
     * "Uncompensated" means no matching ROLLBACK row exists for the same
     * (redemptionId, ruleId, bucketKey) triple.
     */
    List<QuotaEventEntity> findUncompensated(String redemptionId);

    /**
     * Aggregate net counter values per (ruleId, bucketKey, windowStart, windowEnd)
     * for windows whose {@code windowEnd >= threshold}.
     *
     * <p>Returns a map keyed by the string concat {@code "ruleId:bucketKey:epoch"}
     * → net sum of countDelta for that bucket.
     */
    Map<String, Long> aggregateActiveWindowCounters(LocalDateTime threshold);
}
