package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /v1/rules.
 */
public class RegisterRuleRequest {

    @NotBlank(message = "id must not be blank")
    private String id;

    @NotBlank(message = "drl must not be blank")
    private String drl;

    public RegisterRuleRequest() {
    }

    public RegisterRuleRequest(String id, String drl) {
        this.id = id;
        this.drl = drl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDrl() {
        return drl;
    }

    public void setDrl(String drl) {
        this.drl = drl;
    }
}
