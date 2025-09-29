package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.OutboxEvent;

public interface EventPublisherPort {

    void publishBundlePublished(BundlePublishedEvent event);

    void publishWarmupRequested(WarmupRequestedEvent event);

    void publishOutboxEvent(OutboxEvent outboxEvent);

    public static class BundlePublishedEvent {
        private String tenantId;
        private String ruleId;
        private Integer ruleVersion;
        private Integer assignmentVersion;
        private String bundleHash;

        public BundlePublishedEvent() {}

        public BundlePublishedEvent(String tenantId, String ruleId, Integer ruleVersion,
                                   Integer assignmentVersion, String bundleHash) {
            this.tenantId = tenantId;
            this.ruleId = ruleId;
            this.ruleVersion = ruleVersion;
            this.assignmentVersion = assignmentVersion;
            this.bundleHash = bundleHash;
        }

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
    }

    public static class WarmupRequestedEvent {
        private String tenantId;
        private String bundleHash;

        public WarmupRequestedEvent() {}

        public WarmupRequestedEvent(String tenantId, String bundleHash) {
            this.tenantId = tenantId;
            this.bundleHash = bundleHash;
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
    }
}