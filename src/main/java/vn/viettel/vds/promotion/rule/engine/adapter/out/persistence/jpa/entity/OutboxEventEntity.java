package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "outbox_events",
        indexes = {
                @Index(name = "idx_dispatch_queue", columnList = "status, created_at")
        }
)
@Getter
@Setter
public class OutboxEventEntity {

    @Id
    @Column(name = "id", nullable = false, length = 100) // ox_xxx format for event IDs
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private EventType type;

    @ElementCollection
    @CollectionTable(
            name = "outbox_event_payload",
            joinColumns = @JoinColumn(name = "outbox_event_id")
    )
    @MapKeyColumn(name = "payload_key", length = 100)
    @Column(name = "payload_value", columnDefinition = "TEXT")
    private Map<String, String> payload = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_tried_at")
    private Instant lastTriedAt;

    public enum EventType {
        BUNDLE_PUBLISHED,
        WARMUP_REQUESTED
    }

    public enum EventStatus {
        PENDING,
        SENT,
        FAILED
    }
}