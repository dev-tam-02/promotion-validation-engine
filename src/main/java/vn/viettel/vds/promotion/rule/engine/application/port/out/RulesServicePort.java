package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.rule.engine.domain.model.Candidate;

public interface RulesServicePort {

    RuleBundle getRuleBundle(Candidate candidate);

    class RuleBundle {
        private final String bundleHash;
        private final byte[] kieModuleBytes;
        private final String dslVersion;
        private final String ruleVersion;
        private final String assignmentVersion;

        public RuleBundle(String bundleHash, byte[] kieModuleBytes, String dslVersion, String ruleVersion, String assignmentVersion) {
            this.bundleHash = bundleHash;
            this.kieModuleBytes = kieModuleBytes;
            this.dslVersion = dslVersion;
            this.ruleVersion = ruleVersion;
            this.assignmentVersion = assignmentVersion;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public byte[] getKieModuleBytes() {
            return kieModuleBytes;
        }

        public String getDslVersion() {
            return dslVersion;
        }

        public String getRuleVersion() {
            return ruleVersion;
        }

        public String getAssignmentVersion() {
            return assignmentVersion;
        }
    }
}