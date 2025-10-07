package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "bundle_warmups")
@CompoundIndexes({
        @CompoundIndex(name = "byTenantState", def = "{'tenantId': 1, 'state': 1, 'createdAt': 1}")
})
public class BundleWarmup {

    @Id
    private String id; // wu_tenantId_bundleHash format

    private String tenantId;

    private String bundleHash;

    private WarmupState state;

    private Integer attempts;

    private Instant createdAt;

    private Instant updatedAt;

    public BundleWarmup() {
    }

    public BundleWarmup(String id, String tenantId, String bundleHash, WarmupState state,
                        Integer attempts, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.bundleHash = bundleHash;
        this.state = state;
        this.attempts = attempts;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public WarmupState getState() {
        return state;
    }

    public void setState(WarmupState state) {
        this.state = state;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Enum
    public enum WarmupState {
        QUEUED,
        PROCESSING,
        DONE,
        FAILED
    }
}