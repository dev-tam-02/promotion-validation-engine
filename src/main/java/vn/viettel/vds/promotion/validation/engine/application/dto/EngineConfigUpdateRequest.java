package vn.viettel.vds.promotion.validation.engine.application.dto;

import java.util.Map;

public class EngineConfigUpdateRequest {

    private ExecuteConfig execute;
    private CompileConfig compile;

    public EngineConfigUpdateRequest() {}

    public EngineConfigUpdateRequest(ExecuteConfig execute, CompileConfig compile) {
        this.execute = execute;
        this.compile = compile;
    }

    // Getters and Setters
    public ExecuteConfig getExecute() {
        return execute;
    }

    public void setExecute(ExecuteConfig execute) {
        this.execute = execute;
    }

    public CompileConfig getCompile() {
        return compile;
    }

    public void setCompile(CompileConfig compile) {
        this.compile = compile;
    }

    // Nested classes
    public static class ExecuteConfig {
        private Integer timeoutMs;
        private Integer maxRulesFired;
        private Integer maxFacts;
        private Map<String, Double> explainSampling;

        public ExecuteConfig() {}

        public ExecuteConfig(Integer timeoutMs, Integer maxRulesFired, Integer maxFacts,
                           Map<String, Double> explainSampling) {
            this.timeoutMs = timeoutMs;
            this.maxRulesFired = maxRulesFired;
            this.maxFacts = maxFacts;
            this.explainSampling = explainSampling;
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

        public Integer getMaxFacts() {
            return maxFacts;
        }

        public void setMaxFacts(Integer maxFacts) {
            this.maxFacts = maxFacts;
        }

        public Map<String, Double> getExplainSampling() {
            return explainSampling;
        }

        public void setExplainSampling(Map<String, Double> explainSampling) {
            this.explainSampling = explainSampling;
        }
    }

    public static class CompileConfig {
        private Integer maxNodes;
        private Integer maxDepth;

        public CompileConfig() {}

        public CompileConfig(Integer maxNodes, Integer maxDepth) {
            this.maxNodes = maxNodes;
            this.maxDepth = maxDepth;
        }

        public Integer getMaxNodes() {
            return maxNodes;
        }

        public void setMaxNodes(Integer maxNodes) {
            this.maxNodes = maxNodes;
        }

        public Integer getMaxDepth() {
            return maxDepth;
        }

        public void setMaxDepth(Integer maxDepth) {
            this.maxDepth = maxDepth;
        }
    }
}