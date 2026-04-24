package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to rollback Layer-1 quota counters for a redemption")
public class RollbackRequest {

    @Schema(description = "Redemption ID whose Layer-1 counters should be decremented", required = true)
    @NotBlank(message = "redemptionId is required")
    @JsonProperty("redemptionId")
    private String redemptionId;

    public String getRedemptionId() {
        return redemptionId;
    }

    public void setRedemptionId(String redemptionId) {
        this.redemptionId = redemptionId;
    }
}
