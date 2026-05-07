package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.QuotaEventEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuotaEventJpaRepository extends JpaRepository<QuotaEventEntity, String> {

    /**
     * Find all INCR/DECR events for a specific redemption that have not yet been compensated
     * by a ROLLBACK event (used to drive the rollback endpoint).
     */
    @Query("""
            SELECT e FROM QuotaEventEntity e
            WHERE e.redemptionId = :redemptionId
              AND e.eventType IN (vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.QuotaEventEntity.EventType.INCR,
                                  vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.QuotaEventEntity.EventType.DECR)
              AND NOT EXISTS (
                  SELECT 1 FROM QuotaEventEntity rb
                  WHERE rb.redemptionId = e.redemptionId
                    AND rb.ruleId = e.ruleId
                    AND rb.bucketKey = e.bucketKey
                    AND rb.eventType = vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.QuotaEventEntity.EventType.ROLLBACK
              )
            """)
    List<QuotaEventEntity> findUncompensatedByRedemptionId(@Param("redemptionId") String redemptionId);

    /**
     * Aggregate net count-deltas per (ruleId, bucketKey, windowStart) for all events
     * inside active windows (window_end >= threshold), used by the rebuild job.
     */
    @Query("""
            SELECT e.ruleId, e.bucketKey, e.windowStart, e.windowEnd, SUM(e.countDelta)
            FROM QuotaEventEntity e
            WHERE e.windowEnd >= :threshold
            GROUP BY e.ruleId, e.bucketKey, e.windowStart, e.windowEnd
            """)
    List<Object[]> aggregateActiveWindowCounters(@Param("threshold") LocalDateTime threshold);
}
