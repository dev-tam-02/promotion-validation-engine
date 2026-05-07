package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persistent audit log for Layer-1 Redis quota counter operations.
 *
 * <p>Every INCR, DECR, or ROLLBACK applied to a Redis counter is journaled here so
 * that counters can be fully reconstructed from this table after a Redis outage.
 */
@Entity
@Table(name = "quota_events",
        indexes = {
                @Index(name = "idx_qe_rule_window", columnList = "rule_id, bucket_key, window_start"),
                @Index(name = "idx_qe_redemption", columnList = "redemption_id")
        })
@Getter
@Setter
public class QuotaEventEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "rule_id", length = 36, nullable = false)
    private String ruleId;

    @Column(name = "redemption_id", length = 36, nullable = false)
    private String redemptionId;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "bucket_key", length = 200)
    private String bucketKey;

    @Column(name = "window_start")
    private LocalDateTime windowStart;

    @Column(name = "window_end")
    private LocalDateTime windowEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, columnDefinition = "ENUM('INCR','DECR','ROLLBACK')")
    private EventType eventType;

    @Column(name = "count_delta", nullable = false)
    private int countDelta;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Event type matching the ENUM in the DB schema.
     */
    public enum EventType {
        INCR, DECR, ROLLBACK
    }
}
