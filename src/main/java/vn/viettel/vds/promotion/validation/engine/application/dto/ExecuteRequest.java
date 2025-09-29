package vn.viettel.vds.promotion.validation.engine.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class ExecuteRequest {

    @NotBlank
    private String tenantId;

    @NotNull
    @Valid
    private Bundle bundle;

    @NotNull
    private Map<String, Object> context;

    private ExecuteOptions options;

    public ExecuteRequest() {}

    public ExecuteRequest(String tenantId, Bundle bundle, Map<String, Object> context, ExecuteOptions options) {
        this.tenantId = tenantId;
        this.bundle = bundle;
        this.context = context;
        this.options = options;
    }

    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public ExecuteOptions getOptions() {
        return options;
    }

    public void setOptions(ExecuteOptions options) {
        this.options = options;
    }

    // Nested classes
    public static class Bundle {
        @NotBlank
        private String hash;

        private Integer ruleVersion;
        private Integer assignmentVersion;

        public Bundle() {}

        public Bundle(String hash, Integer ruleVersion, Integer assignmentVersion) {
            this.hash = hash;
            this.ruleVersion = ruleVersion;
            this.assignmentVersion = assignmentVersion;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public Integer getRuleVersion() {
            return ruleVersion;
        }

        public void setRuleVersion(Integer ruleVersion) {
            this.ruleVersion = ruleVersion;
        }

        public Integer getAssignmentVersion() {
            return assignmentVersion;
        }

        public void setAssignmentVersion(Integer assignmentVersion) {
            this.assignmentVersion = assignmentVersion;
        }
    }

    public static class ExecuteOptions {
        private String explain;
        private Integer timeoutMs;
        private Integer maxRulesFired;

        public ExecuteOptions() {}

        public ExecuteOptions(String explain, Integer timeoutMs, Integer maxRulesFired) {
            this.explain = explain;
            this.timeoutMs = timeoutMs;
            this.maxRulesFired = maxRulesFired;
        }

        public String getExplain() {
            return explain;
        }

        public void setExplain(String explain) {
            this.explain = explain;
        }

        public Integer getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(Integer timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public Integer getMaxRulesFired() {
            return maxRulesFired;
        }

        public void setMaxRulesFired(Integer maxRulesFired) {
            this.maxRulesFired = maxRulesFired;
        }
    }
}