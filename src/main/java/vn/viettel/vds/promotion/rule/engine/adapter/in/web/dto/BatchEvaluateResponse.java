package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Batch evaluate response — one result per subject")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchEvaluateResponse {

    @JsonProperty("results")
    private List<SubjectResult> results;

    public BatchEvaluateResponse() {
    }

    public BatchEvaluateResponse(List<SubjectResult> results) {
        this.results = results;
    }

    public List<SubjectResult> getResults() {
        return results;
    }

    public void setResults(List<SubjectResult> results) {
        this.results = results;
    }

    @Schema(description = "Decision for a single subject")
    public static class SubjectResult {
        @JsonProperty("subjectType")
        private String subjectType;

        @JsonProperty("subjectKey")
        private String subjectKey;

        @JsonProperty("bundleHash")
        private String bundleHash;

        /**
         * ALLOW or DENY.
         */
        @JsonProperty("decision")
        private String decision;

        @JsonProperty("ok")
        private Boolean ok;

        @JsonProperty("stage")
        private String stage;

        @JsonProperty("reasonCodes")
        private List<String> reasonCodes;

        @JsonProperty("explanation")
        private String explanation;

        @JsonProperty("latencyMs")
        private Long latencyMs;

        public String getSubjectType() {
            return subjectType;
        }

        public void setSubjectType(String subjectType) {
            this.subjectType = subjectType;
        }

        public String getSubjectKey() {
            return subjectKey;
        }

        public void setSubjectKey(String subjectKey) {
            this.subjectKey = subjectKey;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public void setBundleHash(String bundleHash) {
            this.bundleHash = bundleHash;
        }

        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }

        public Boolean getOk() {
            return ok;
        }

        public void setOk(Boolean ok) {
            this.ok = ok;
        }

        public String getStage() {
            return stage;
        }

        public void setStage(String stage) {
            this.stage = stage;
        }

        public List<String> getReasonCodes() {
            return reasonCodes;
        }

        public void setReasonCodes(List<String> reasonCodes) {
            this.reasonCodes = reasonCodes;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public Long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Long latencyMs) {
            this.latencyMs = latencyMs;
        }
    }
}
