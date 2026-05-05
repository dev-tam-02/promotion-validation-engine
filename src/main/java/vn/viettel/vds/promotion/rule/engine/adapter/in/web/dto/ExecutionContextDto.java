package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Execution context for rule evaluation. {@code now} and {@code timezone} are
 * OPTIONAL on the wire — server-side callers (BatchEvaluateService /
 * OrderValidationService) apply sensible defaults via
 * {@link #effectiveNow()} / {@link #effectiveTimezone()} when omitted, so a
 * client mismatch never blocks the redemption pipeline (BUG-020).
 */
@Schema(description = "Execution context for rule evaluation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionContextDto {

    private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

    @Schema(description = "Current timestamp; defaults to server time if absent", example = "2025-09-20T10:00:00Z")
    @JsonProperty("now")
    private Instant now;

    @Schema(description = "Timezone for evaluation; defaults to Asia/Ho_Chi_Minh if absent", example = "Asia/Bangkok")
    @JsonProperty("timezone")
    private String timezone;

    @Schema(description = "Session identifier", example = "session123")
    @JsonProperty("sessionId")
    private String sessionId;

    @Schema(description = "Additional variables")
    @JsonProperty("variables")
    private Map<String, Object> variables;

    // Constructors
    public ExecutionContextDto() {
    }

    public ExecutionContextDto(Instant now, String timezone) {
        this.now = now;
        this.timezone = timezone;
    }

    // Getters and setters
    public Instant getNow() {
        return now;
    }

    public void setNow(Instant now) {
        this.now = now;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    /**
     * Returns {@link #getNow()} if set, otherwise the server's current instant.
     * Use this from server-side code instead of {@code getNow()} so a missing
     * client-side {@code now} never short-circuits evaluation (BUG-020).
     */
    public Instant effectiveNow() {
        return now != null ? now : Instant.now();
    }

    /**
     * Returns {@link #getTimezone()} if set, otherwise the redemption-default
     * {@code Asia/Ho_Chi_Minh}. Companion to {@link #effectiveNow()}.
     */
    public String effectiveTimezone() {
        return (timezone != null && !timezone.isBlank()) ? timezone : DEFAULT_TIMEZONE;
    }
}