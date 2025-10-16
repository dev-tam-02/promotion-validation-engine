package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.validation.engine.domain.model.Candidate;
import vn.viettel.vds.promotion.validation.engine.domain.model.Customer;
import vn.viettel.vds.promotion.validation.engine.domain.model.Order;
import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;

import java.util.List;

public interface RuleEnginePort {

    ValidationResult executeRules(Customer customer, Order order, Candidate candidate, String bundleHash);

    List<ValidationResult> executeBulkRules(Customer customer, Order order, List<Candidate> candidates);

    void loadRuleBundle(String bundleHash, byte[] kieModuleBytes);

    boolean isRuleBundleLoaded(String bundleHash);

    CompileResult compile(CompileInput input);

    ExecuteResponse execute(ExecuteInput input);

    List<ExecuteResponse> executeBatch(List<ExecuteInput> inputs);

    void warmupBundle(String bundleHash, byte[] artifactBytes);

    boolean isBundleCached(String bundleHash);

    class CompileInput {
        private String tenantId;
        private String ruleId;
        private Integer version;
        private List<java.util.Map<String, Object>> nodes;
        private String operatorsFingerprint;
        private String compilerId;

        public CompileInput() {
        }

        public CompileInput(String tenantId, String ruleId, Integer version, List<java.util.Map<String, Object>> nodes,
                            String operatorsFingerprint, String compilerId) {
            this.tenantId = tenantId;
            this.ruleId = ruleId;
            this.version = version;
            this.nodes = nodes;
            this.operatorsFingerprint = operatorsFingerprint;
            this.compilerId = compilerId;
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

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public List<java.util.Map<String, Object>> getNodes() {
            return nodes;
        }

        public void setNodes(List<java.util.Map<String, Object>> nodes) {
            this.nodes = nodes;
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
    }

    class CompileResult {
        private final String bundleHash;
        private final byte[] artifactBytes;
        private final Long size;
        private final List<String> logs;
        private final String droolsVersion;
        private final String drlContent;

        public CompileResult(String bundleHash, byte[] artifactBytes, Long size, List<String> logs, String droolsVersion, String drlContent) {
            this.bundleHash = bundleHash;
            this.artifactBytes = artifactBytes;
            this.size = size;
            this.logs = logs;
            this.droolsVersion = droolsVersion;
            this.drlContent = drlContent;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public byte[] getArtifactBytes() {
            return artifactBytes;
        }

        public Long getSize() {
            return size;
        }

        public List<String> getLogs() {
            return logs;
        }

        public String getDroolsVersion() {
            return droolsVersion;
        }

        public String getDrlContent() {
            return drlContent;
        }
    }

    class ExecuteInput {
        private String tenantId;
        private String bundleHash;
        private java.util.Map<String, Object> context;
        private ExecuteOptions options;

        public ExecuteInput() {
        }

        public ExecuteInput(String tenantId, String bundleHash, java.util.Map<String, Object> context, ExecuteOptions options) {
            this.tenantId = tenantId;
            this.bundleHash = bundleHash;
            this.context = context;
            this.options = options;
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

        public java.util.Map<String, Object> getContext() {
            return context;
        }

        public void setContext(java.util.Map<String, Object> context) {
            this.context = context;
        }

        public ExecuteOptions getOptions() {
            return options;
        }

        public void setOptions(ExecuteOptions options) {
            this.options = options;
        }
    }

    class ExecuteOptions {
        private String explain;
        private Integer timeoutMs;
        private Integer maxRulesFired;

        public ExecuteOptions() {
        }

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