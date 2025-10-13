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

    @NotBlank
    private String logic;

    @NotNull
    private List<Map<String, Object>> nodes;

    private String operatorsFingerprint;

    @Valid
    private Limits limits;

    private List<TimeLink> timeLinks;

    public CompileRequest() {
        // Used by frameworks for deserialization.
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

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
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
}