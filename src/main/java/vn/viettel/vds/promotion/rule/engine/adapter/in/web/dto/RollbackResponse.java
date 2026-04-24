package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a Layer-1 quota rollback operation")
public class RollbackResponse {

    @Schema(description = "Redemption ID that was rolled back")
    private String redemptionId;

    @Schema(description = "Number of counter entries that were decremented")
    private int revertedCount;

    @Schema(description = "Outcome status: REVERTED or NO_OP (no uncompensated events found)")
    private String status;

    public static RollbackResponse of(String redemptionId, int revertedCount) {
        RollbackResponse r = new RollbackResponse();
        r.redemptionId  = redemptionId;
        r.revertedCount = revertedCount;
        r.status        = revertedCount > 0 ? "REVERTED" : "NO_OP";
        return r;
    }

    public String getRedemptionId() { return redemptionId; }
    public int getRevertedCount()   { return revertedCount; }
    public String getStatus()       { return status; }
}
