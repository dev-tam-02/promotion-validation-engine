package vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity;

import com.promix.platform.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
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
@EntityListeners(IdGenerationListener.class)
@AttributeOverride(name = "id", column = @Column(name = "id", nullable = false, length = 100))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class OutboxEventEntity extends BaseEntity {

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
