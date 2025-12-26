package vn.viettel.vds.promotion.rule.engine.application.dto;

import java.util.List;

public class LatestBundleResponse {

    private String ruleId;
    private Integer ruleVersion;
    private Integer assignmentVersion;
    private String bundleHash;
    private Limits limits;
    private List<TimeLink> timeLinks;

    public LatestBundleResponse() {
    }

    public LatestBundleResponse(String ruleId, Integer ruleVersion, Integer assignmentVersion,
                                String bundleHash, Limits limits, List<TimeLink> timeLinks) {
        this.ruleId = ruleId;
        this.ruleVersion = ruleVersion;
        this.assignmentVersion = assignmentVersion;
        this.bundleHash = bundleHash;
        this.limits = limits;
        this.timeLinks = timeLinks;
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

    public Integer getAssignmentVersion() {
        return assignmentVersion;
    }

    public void setAssignmentVersion(Integer assignmentVersion) {
        this.assignmentVersion = assignmentVersion;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
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
}