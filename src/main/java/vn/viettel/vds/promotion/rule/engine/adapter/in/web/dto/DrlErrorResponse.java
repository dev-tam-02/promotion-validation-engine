package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import java.util.List;

/**
 * Error response body for DRL-related failures.
 *
 * <p>Error codes:
 * <ul>
 *   <li>{@code DRL_COMPILE_ERROR} — 400: DRL has syntax / semantic errors.</li>
 *   <li>{@code RULE_CONFLICT}     — 409: POST with existing id but different DRL content.</li>
 *   <li>{@code RULE_NOT_FOUND}    — 404: PUT or DELETE targeting an unknown id.</li>
 * </ul>
 */
public class DrlErrorResponse {

    private String code;
    private String message;
    private List<String> details;

    public DrlErrorResponse() {
    }

    public DrlErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public DrlErrorResponse(String code, String message, List<String> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}
