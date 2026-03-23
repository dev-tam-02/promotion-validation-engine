package vn.viettel.vds.promotion.rule.engine.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class WarmupRequest {

    @NotEmpty
    private List<String> bundleHashes;

    public WarmupRequest() {
    }

    public WarmupRequest(List<String> bundleHashes) {
        this.bundleHashes = bundleHashes;
    }

    // Getters and Setters
    public List<String> getBundleHashes() {
        return bundleHashes;
    }

    public void setBundleHashes(List<String> bundleHashes) {
        this.bundleHashes = bundleHashes;
    }
}