package vn.viettel.vds.promotion.rule.engine.domain.service.bundle;

import java.time.Instant;

public class ActiveRuleInfo {
    private final String ruleId;
    private final String bundleHash;
    private final byte[] compiledBytes;
    private final Integer version;
    private final Instant lastUpdated;

    public ActiveRuleInfo(String ruleId, String bundleHash, byte[] compiledBytes,
                          Integer version, Instant lastUpdated) {
        this.ruleId = ruleId;
        this.bundleHash = bundleHash;
        this.compiledBytes = compiledBytes;
        this.version = version;
        this.lastUpdated = lastUpdated;
    }

    // Getters
    public String getRuleId() {
        return ruleId;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public byte[] getCompiledBytes() {
        return compiledBytes;
    }

    public Integer getVersion() {
        return version;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }
}