package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.util.Map;

public class ReasonCode {
    private String code;
    private Map<String, Object> params;

    public ReasonCode() {
    }

    public ReasonCode(String code) {
        this.code = code;
    }

    public ReasonCode(String code, Map<String, Object> params) {
        this.code = code;
        this.params = params;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}