package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.RollbackRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.RollbackResponse;
import vn.viettel.vds.promotion.rule.engine.application.service.QuotaRollbackService;

/**
 * Exposes {@code POST /v1/rules/rollback} — Layer-1 compensation endpoint.
 *
 * <p>Called by pp-redemption (saga compensation step C5) to decrement Redis counters
 * that were incremented during a failed or rolled-back redemption.
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/rules")
@Tag(name = "Rule Evaluation", description = "Rule evaluation and simulation API")
public class RollbackController {

    private static final Logger log = LoggerFactory.getLogger(RollbackController.class);

    private final QuotaRollbackService rollbackService;

    public RollbackController(QuotaRollbackService rollbackService) {
        this.rollbackService = rollbackService;
    }

    @Operation(
            summary = "Rollback Layer-1 quota counters for a redemption",
            description = "Finds all uncompensated INCR quota_events for the given redemptionId, "
                    + "decrements the corresponding Redis counters, and inserts ROLLBACK rows. "
                    + "Idempotent: repeated calls for an already-rolled-back redemption return NO_OP.")
    @PostMapping("/rollback")
    public ResponseEntity<RollbackResponse> rollback(@Valid @RequestBody RollbackRequest request) {
        log.info("POST /v1/rules/rollback redemptionId={}", request.getRedemptionId());
        int reverted = rollbackService.rollback(request.getRedemptionId());
        RollbackResponse response = RollbackResponse.of(request.getRedemptionId(), reverted);
        log.info("Rollback complete: redemptionId={} revertedCount={} status={}",
                request.getRedemptionId(), reverted, response.getStatus());
        return ResponseEntity.ok(response);
    }
}
