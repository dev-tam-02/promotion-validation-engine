package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of a rule evaluation, with full trace when mode=SIMULATE")
public class EvaluateRuleResponse {

    @Schema(description = "Overall verdict: ALLOW or DENY", example = "ALLOW")
    private String verdict;

    @Schema(description = "Per-node trace entries; populated only when mode=SIMULATE")
    private List<TraceEntry> trace;

    @Schema(description = "Node IDs (or Drools declaration IDs) that satisfied their conditions")
    private List<String> matchedNodes;

    @Schema(description = "Node IDs that did not satisfy their conditions")
    private List<String> unmatchedNodes;

    @Schema(description = "Reason codes emitted by fired DENY rules")
    private List<String> reasonCodes;

    public EvaluateRuleResponse() {
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public List<TraceEntry> getTrace() {
        return trace;
    }

    public void setTrace(List<TraceEntry> trace) {
        this.trace = trace;
    }

    public List<String> getMatchedNodes() {
        return matchedNodes;
    }

    public void setMatchedNodes(List<String> matchedNodes) {
        this.matchedNodes = matchedNodes;
    }

    public List<String> getUnmatchedNodes() {
        return unmatchedNodes;
    }

    public void setUnmatchedNodes(List<String> unmatchedNodes) {
        this.unmatchedNodes = unmatchedNodes;
    }

    public List<String> getReasonCodes() {
        return reasonCodes;
    }

    public void setReasonCodes(List<String> reasonCodes) {
        this.reasonCodes = reasonCodes;
    }
}
