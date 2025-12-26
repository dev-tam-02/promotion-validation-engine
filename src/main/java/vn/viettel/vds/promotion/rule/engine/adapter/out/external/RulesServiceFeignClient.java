package vn.viettel.vds.promotion.rule.engine.adapter.out.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.viettel.vds.promotion.rule.engine.adapter.out.config.FeignConfiguration;

@FeignClient(
        name = "rules-service",
        url = "${validation.external-services.rules-service.base-url:http://rules-service}",
        configuration = FeignConfiguration.class
)
public interface RulesServiceFeignClient {

    @GetMapping("/api/rule-bundles/{type}/{code}")
    RuleBundleResponse getRuleBundle(@PathVariable("type") String type, @PathVariable("code") String code);

    class RuleBundleResponse {
        private String bundleHash;
        private byte[] kieModuleBytes;
        private String dslVersion;
        private String ruleVersion;
        private String assignmentVersion;

        public RuleBundleResponse() {
        }

        public RuleBundleResponse(String bundleHash, byte[] kieModuleBytes, String dslVersion, String ruleVersion, String assignmentVersion) {
            this.bundleHash = bundleHash;
            this.kieModuleBytes = kieModuleBytes;
            this.dslVersion = dslVersion;
            this.ruleVersion = ruleVersion;
            this.assignmentVersion = assignmentVersion;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public void setBundleHash(String bundleHash) {
            this.bundleHash = bundleHash;
        }

        public byte[] getKieModuleBytes() {
            return kieModuleBytes;
        }

        public void setKieModuleBytes(byte[] kieModuleBytes) {
            this.kieModuleBytes = kieModuleBytes;
        }

        public String getDslVersion() {
            return dslVersion;
        }

        public void setDslVersion(String dslVersion) {
            this.dslVersion = dslVersion;
        }

        public String getRuleVersion() {
            return ruleVersion;
        }

        public void setRuleVersion(String ruleVersion) {
            this.ruleVersion = ruleVersion;
        }

        public String getAssignmentVersion() {
            return assignmentVersion;
        }

        public void setAssignmentVersion(String assignmentVersion) {
            this.assignmentVersion = assignmentVersion;
        }
    }
}