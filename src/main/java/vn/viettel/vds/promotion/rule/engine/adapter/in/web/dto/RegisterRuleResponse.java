package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

/**
 * Response body for POST /v1/rules (201) and PUT /v1/rules/{id} (200).
 */
public class RegisterRuleResponse {

    private String ruleId;
    private String bundleHash;

    public RegisterRuleResponse() {
    }

    public RegisterRuleResponse(String ruleId, String bundleHash) {
        this.ruleId = ruleId;
        this.bundleHash = bundleHash;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }
}
