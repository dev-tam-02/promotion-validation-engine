package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Single node trace entry produced during SIMULATE evaluation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TraceEntry {

    @Schema(description = "Node identifier — corresponds to the fact variable bound by the rule pattern", example = "$order")
    private String nodeId;

    @Schema(description = "Node type: COND (leaf condition) or GROUP (logical combinator)", example = "COND")
    private String type;

    @Schema(description = "Operator or logical function name", example = "order.total.gte")
    private String operator;

    @Schema(description = "Whether this node evaluated to true (condition satisfied / group passed)", example = "true")
    private boolean result;

    @Schema(description = "Reason code if the node caused a DENY verdict; null when result=true or neutral")
    private String reason;

    public TraceEntry() {
    }

    public TraceEntry(String nodeId, String type, String operator, boolean result, String reason) {
        this.nodeId = nodeId;
        this.type = type;
        this.operator = operator;
        this.result = result;
        this.reason = reason;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
