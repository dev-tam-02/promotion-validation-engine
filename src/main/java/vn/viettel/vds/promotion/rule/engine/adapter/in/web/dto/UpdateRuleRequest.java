package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for PUT /v1/rules/{id}.
 */
public class UpdateRuleRequest {

    @NotBlank(message = "drl must not be blank")
    private String drl;

    public UpdateRuleRequest() {
    }

    public UpdateRuleRequest(String drl) {
        this.drl = drl;
    }

    public String getDrl() {
        return drl;
    }

    public void setDrl(String drl) {
        this.drl = drl;
    }
}
