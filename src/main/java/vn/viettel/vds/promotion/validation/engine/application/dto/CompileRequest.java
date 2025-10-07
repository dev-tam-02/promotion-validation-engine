package vn.viettel.vds.promotion.validation.engine.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

public class CompileRequest {

    @NotBlank
    private String tenantId;

    @NotBlank
    private String ruleId;

    @NotNull
    @Positive
    private Integer version;

    @NotNull
    private List<Map<String, Object>> nodes;

    @Valid
    private Limits limits;

    private List<TimeLink> timeLinks;

    @NotBlank
    private String operatorsFingerprint;

    @NotBlank
    private String compilerId;

    @Valid
    private Source source;

    public CompileRequest() {
    }

    public CompileRequest(String tenantId, String ruleId, Integer version, List<Map<String, Object>> nodes,
                          Limits limits, List<TimeLink> timeLinks, String operatorsFingerprint,
                          String compilerId, Source source) {
        this.tenantId = tenantId;
        this.ruleId = ruleId;
        this.version = version;
        this.nodes = nodes;
        this.limits = limits;
        this.timeLinks = timeLinks;
        this.operatorsFingerprint = operatorsFingerprint;
        this.compilerId = compilerId;
        this.source = source;
    }

    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<Map<String, Object>> getNodes() {
        return nodes;
    }

    public void setNodes(List<Map<String, Object>> nodes) {
        this.nodes = nodes;
    }

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits;
    }

    public List<TimeLink> getTimeLinks() {
        return timeLinks;
    }

    public void setTimeLinks(List<TimeLink> timeLinks) {
        this.timeLinks = timeLinks;
    }

    public String getOperatorsFingerprint() {
        return operatorsFingerprint;
    }

    public void setOperatorsFingerprint(String operatorsFingerprint) {
        this.operatorsFingerprint = operatorsFingerprint;
    }

    public String getCompilerId() {
        return compilerId;
    }

    public void setCompilerId(String compilerId) {
        this.compilerId = compilerId;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    // Nested classes
    public static class Limits {
        private Integer perCustomer;
        private Integer perDay;

        public Limits() {
        }

        public Limits(Integer perCustomer, Integer perDay) {
            this.perCustomer = perCustomer;
            this.perDay = perDay;
        }

        public Integer getPerCustomer() {
            return perCustomer;
        }

        public void setPerCustomer(Integer perCustomer) {
            this.perCustomer = perCustomer;
        }

        public Integer getPerDay() {
            return perDay;
        }

        public void setPerDay(Integer perDay) {
            this.perDay = perDay;
        }
    }

    public static class TimeLink {
        @NotBlank
        private String policyId;

        @NotBlank
        private String mode;

        public TimeLink() {
        }

        public TimeLink(String policyId, String mode) {
            this.policyId = policyId;
            this.mode = mode;
        }

        public String getPolicyId() {
            return policyId;
        }

        public void setPolicyId(String policyId) {
            this.policyId = policyId;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class Source {
        @NotBlank
        private String ruleVersionId;

        @NotBlank
        private String snapshotHash;

        public Source() {
        }

        public Source(String ruleVersionId, String snapshotHash) {
            this.ruleVersionId = ruleVersionId;
            this.snapshotHash = snapshotHash;
        }

        public String getRuleVersionId() {
            return ruleVersionId;
        }

        public void setRuleVersionId(String ruleVersionId) {
            this.ruleVersionId = ruleVersionId;
        }

        public String getSnapshotHash() {
            return snapshotHash;
        }

        public void setSnapshotHash(String snapshotHash) {
            this.snapshotHash = snapshotHash;
        }
    }
}