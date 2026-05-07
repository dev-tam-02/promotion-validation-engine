package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Quota window context supplied with a {@code mode=REDEMPTION} evaluate request.
 *
 * <p>When present, the evaluate endpoint performs Layer-1 counter enforcement:
 * if the INCR would exceed {@code limit}, the request is denied with
 * {@code MAX_USES_EXCEEDED}.
 */
@Schema(description = "Quota window context for REDEMPTION mode evaluation")
public class QuotaContext {

    @Schema(description = "Customer ID — part of the counter bucket key for per-customer limits")
    @JsonProperty("customerId")
    private String customerId;

    @Schema(description = "Bucket key for the quota counter (e.g. '{customerId}' or '{configId}')")
    @JsonProperty("bucketKey")
    private String bucketKey;

    @Schema(description = "Start of the quota window (inclusive)")
    @JsonProperty("windowStart")
    private LocalDateTime windowStart;

    @Schema(description = "End of the quota window (exclusive) — TTL base for Redis key")
    @JsonProperty("windowEnd")
    private LocalDateTime windowEnd;

    @Schema(description = "Maximum allowed count within the window (0 = unlimited)")
    @JsonProperty("limit")
    private int limit;

    // ---- getters / setters ----

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String v) {
        this.customerId = v;
    }

    public String getBucketKey() {
        return bucketKey;
    }

    public void setBucketKey(String v) {
        this.bucketKey = v;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDateTime v) {
        this.windowStart = v;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalDateTime v) {
        this.windowEnd = v;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int v) {
        this.limit = v;
    }
}
