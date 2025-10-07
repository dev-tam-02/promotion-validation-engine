package vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for fast check evaluation.
 */
@Schema(description = "Fast check evaluation response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FastCheckResponse {

    @Schema(description = "Validation decision", example = "ALLOW", required = true, allowableValues = {"ALLOW", "DENY"})
    @JsonProperty("decision")
    private String decision;

    @Schema(description = "Should continue to full evaluation", example = "true", required = true)
    @JsonProperty("shouldContinue")
    private boolean shouldContinue;

    @Schema(description = "Primary reason code if denied", example = "ORDER_VALUE_TOO_LOW")
    @JsonProperty("reasonCode")
    private String reasonCode;

    @Schema(description = "Human-readable explanation", example = "Order value 50000 below minimum 100000")
    @JsonProperty("explanation")
    private String explanation;

    @Schema(description = "List of all failed checks")
    @JsonProperty("failedChecks")
    private List<String> failedChecks;

    @Schema(description = "Execution latency in milliseconds", example = "3")
    @JsonProperty("latencyMs")
    private Long latencyMs;

    @Schema(description = "Additional metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public FastCheckResponse() {
    }

    public FastCheckResponse(String decision, boolean shouldContinue) {
        this.decision = decision;
        this.shouldContinue = shouldContinue;
    }

    // Factory methods
    public static FastCheckResponse allow(String explanation) {
        FastCheckResponse response = new FastCheckResponse();
        response.setDecision("ALLOW");
        response.setShouldContinue(true);
        response.setExplanation(explanation);
        return response;
    }

    public static FastCheckResponse deny(String reasonCode, String explanation) {
        FastCheckResponse response = new FastCheckResponse();
        response.setDecision("DENY");
        response.setShouldContinue(false);
        response.setReasonCode(reasonCode);
        response.setExplanation(explanation);
        return response;
    }

    // Getters and setters
    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public boolean isShouldContinue() {
        return shouldContinue;
    }

    public void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<String> getFailedChecks() {
        return failedChecks;
    }

    public void setFailedChecks(List<String> failedChecks) {
        this.failedChecks = failedChecks;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}