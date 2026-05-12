package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@Schema(description = "Request for rule evaluation (including SIMULATE and REDEMPTION modes)")
public class EvaluateRuleRequest {

    @Schema(description = "Evaluation mode: NORMAL, SIMULATE, or REDEMPTION",
            example = "SIMULATE", allowableValues = {"NORMAL", "SIMULATE", "REDEMPTION"})
    @NotNull(message = "mode is required")
    @JsonProperty("mode")
    private String mode = "NORMAL";

    @Schema(description = "Redemption ID — required when mode=REDEMPTION; correlates quota_events rows")
    @JsonProperty("redemptionId")
    private String redemptionId;

    @Schema(description = "Quota window context for Layer-1 counter enforcement — used when mode=REDEMPTION")
    @JsonProperty("quotaContext")
    private QuotaContext quotaContext;

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

    public String getRedemptionId() {
        return redemptionId;
    }

    public void setRedemptionId(String redemptionId) {
        this.redemptionId = redemptionId;
    }

    public QuotaContext getQuotaContext() {
        return quotaContext;
    }

    public void setQuotaContext(QuotaContext quotaContext) {
        this.quotaContext = quotaContext;
    }

    public boolean isSimulateMode() {
        return "SIMULATE".equalsIgnoreCase(mode);
    }

    public boolean isRedemptionMode() {
        return "REDEMPTION".equalsIgnoreCase(mode);
    }
}
