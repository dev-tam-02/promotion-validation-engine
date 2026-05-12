package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.time.Instant;

/**
 * Domain model representing a DRL rule registered directly via the /v1/rules CRUD API.
 *
 * <p>Rules registered here bypass the structured node→DRL translation pipeline
 * (used by /v1/compile) and accept raw DRL authored by pp-validation.
 * The bundleHash is deterministic: SHA-256 of the DRL content, prefixed with "sha256:".
 *
 * <p>Persistence: in-memory (MVP). Task 09 V2 will migrate to {@code rule_registry} DB table.
 */
public class RegisteredRule {

    private final String ruleId;
    private final String drl;
    private final String bundleHash;
    private final Instant registeredAt;
    private final Instant updatedAt;

    public RegisteredRule(String ruleId, String drl, String bundleHash,
                          Instant registeredAt, Instant updatedAt) {
        this.ruleId = ruleId;
        this.drl = drl;
        this.bundleHash = bundleHash;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Returns true if the given DRL content is identical to the stored DRL.
     * Used for idempotency check on POST.
     */
    public boolean hasSameDrl(String other) {
        return this.drl.equals(other);
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getDrl() {
        return drl;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
