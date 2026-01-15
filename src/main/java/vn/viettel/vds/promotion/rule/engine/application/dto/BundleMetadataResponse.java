package vn.viettel.vds.promotion.rule.engine.application.dto;

import java.util.List;

public class BundleMetadataResponse {

    private String ruleId;
    private Integer ruleVersion;
    private String operatorsFingerprint;
    private Limits limits;
    private List<TimeLink> timeLinks;
    private Engine engine;

    public BundleMetadataResponse() {
    }

    public BundleMetadataResponse(String ruleId, Integer ruleVersion,
                                  String operatorsFingerprint, Limits limits, List<TimeLink> timeLinks,
                                  Engine engine) {
        this.ruleId = ruleId;
        this.ruleVersion = ruleVersion;
        this.operatorsFingerprint = operatorsFingerprint;
        this.limits = limits;
        this.timeLinks = timeLinks;
        this.engine = engine;
    }

    // Getters and Setters
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(Integer ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getOperatorsFingerprint() {
        return operatorsFingerprint;
    }

    public void setOperatorsFingerprint(String operatorsFingerprint) {
        this.operatorsFingerprint = operatorsFingerprint;
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

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
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
        private String policyId;
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

    public static class Engine {
        private String type;
        private String compilerId;
        private String droolsVersion;

        public Engine() {
        }

        public Engine(String type, String compilerId, String droolsVersion) {
            this.type = type;
            this.compilerId = compilerId;
            this.droolsVersion = droolsVersion;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCompilerId() {
            return compilerId;
        }

        public void setCompilerId(String compilerId) {
            this.compilerId = compilerId;
        }

        public String getDroolsVersion() {
            return droolsVersion;
        }

        public void setDroolsVersion(String droolsVersion) {
            this.droolsVersion = droolsVersion;
        }
    }
}