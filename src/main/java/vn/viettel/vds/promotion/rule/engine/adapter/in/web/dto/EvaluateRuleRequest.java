package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@Schema(description = "Request for rule evaluation (including SIMULATE mode)")
public class EvaluateRuleRequest {

    @Schema(description = "Evaluation mode: NORMAL or SIMULATE", example = "SIMULATE", allowableValues = {"NORMAL", "SIMULATE"})
    @NotNull(message = "mode is required")
    @JsonProperty("mode")
    private String mode = "NORMAL";

    @Schema(description = "Fact map: keys are fact type names (order, customer, candidate, voucher), "
            + "values are objects matching those types")
    @NotNull(message = "facts is required")
    @JsonProperty("facts")
    private Map<String, Object> facts;

    @Schema(description = "Rule IDs to evaluate (must be registered via POST /v1/rules first)")
    @NotEmpty(message = "ruleIds must not be empty")
    @JsonProperty("ruleIds")
    private List<String> ruleIds;

    // ------ getters & setters ------

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Map<String, Object> getFacts() {
        return facts;
    }

    public void setFacts(Map<String, Object> facts) {
        this.facts = facts;
    }

    public List<String> getRuleIds() {
        return ruleIds;
    }

    public void setRuleIds(List<String> ruleIds) {
        this.ruleIds = ruleIds;
    }

    public boolean isSimulateMode() {
        return "SIMULATE".equalsIgnoreCase(mode);
    }
}
