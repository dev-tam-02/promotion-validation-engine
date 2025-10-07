package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "outbox_events")
@CompoundIndexes({
        @CompoundIndex(name = "dispatch_queue", def = "{'tenantId': 1, 'status': 1, 'createdAt': 1}")
})
public class OutboxEvent {

    @Id
    private String id; // ox_xxx format

    private String tenantId;

    private EventType type;

    private Map<String, Object> payload;

    private EventStatus status;

    private Integer attempts;

    private Instant createdAt;

    private Instant lastTriedAt;

    public OutboxEvent() {
    }

    public OutboxEvent(String id, String tenantId, EventType type, Map<String, Object> payload,
                       EventStatus status, Integer attempts, Instant createdAt, Instant lastTriedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.type = type;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.createdAt = createdAt;
        this.lastTriedAt = lastTriedAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastTriedAt() {
        return lastTriedAt;
    }

    public void setLastTriedAt(Instant lastTriedAt) {
        this.lastTriedAt = lastTriedAt;
    }

    // Enums
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