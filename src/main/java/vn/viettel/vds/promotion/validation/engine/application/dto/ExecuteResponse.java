package vn.viettel.vds.promotion.validation.engine.application.dto;

import java.util.List;

public class ExecuteResponse {

    private Boolean ok;
    private String decision;
    private List<String> reasonCodes;
    private List<ExplainEntry> explain;
    private Engine engine;

    public ExecuteResponse() {
    }

    public ExecuteResponse(Boolean ok, String decision, List<String> reasonCodes,
                           List<ExplainEntry> explain, Engine engine) {
        this.ok = ok;
        this.decision = decision;
        this.reasonCodes = reasonCodes;
        this.explain = explain;
        this.engine = engine;
    }

    // Getters and Setters
    public Boolean getOk() {
        return ok;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public List<String> getReasonCodes() {
        return reasonCodes;
    }

    public void setReasonCodes(List<String> reasonCodes) {
        this.reasonCodes = reasonCodes;
    }

    public List<ExplainEntry> getExplain() {
        return explain;
    }

    public void setExplain(List<ExplainEntry> explain) {
        this.explain = explain;
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    // Nested classes
    public static class ExplainEntry {
        private String node;
        private String operator;
        private Boolean result;

        public ExplainEntry() {
        }

        public ExplainEntry(String node, String operator, Boolean result) {
            this.node = node;
            this.operator = operator;
            this.result = result;
        }

        public String getNode() {
            return node;
        }

        public void setNode(String node) {
            this.node = node;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public Boolean getResult() {
            return result;
        }

        public void setResult(Boolean result) {
            this.result = result;
        }
    }

    public static class Engine {
        private String version;
        private Integer latencyMs;
        private Boolean cacheHit;

        public Engine() {
        }

        public Engine(String version, Integer latencyMs, Boolean cacheHit) {
            this.version = version;
            this.latencyMs = latencyMs;
            this.cacheHit = cacheHit;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Integer getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Boolean getCacheHit() {
            return cacheHit;
        }

        public void setCacheHit(Boolean cacheHit) {
            this.cacheHit = cacheHit;
        }
    }
}