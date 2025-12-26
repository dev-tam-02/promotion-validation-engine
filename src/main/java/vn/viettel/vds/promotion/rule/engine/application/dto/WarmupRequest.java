package vn.viettel.vds.promotion.rule.engine.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class WarmupRequest {

    @NotBlank
    private String tenantId;

    @NotEmpty
    private List<String> bundleHashes;

    public WarmupRequest() {
    }

    public WarmupRequest(String tenantId, List<String> bundleHashes) {
        this.tenantId = tenantId;
        this.bundleHashes = bundleHashes;
    }

    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<String> getBundleHashes() {
        return bundleHashes;
    }

    public void setBundleHashes(List<String> bundleHashes) {
        this.bundleHashes = bundleHashes;
    }
}