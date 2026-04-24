package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.EvaluateRuleRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.EvaluateRuleResponse;
import vn.viettel.vds.promotion.rule.engine.application.usecase.SimulateEvaluationService;

/**
 * Exposes {@code POST /v1/rules/evaluate} — a unified endpoint for both normal evaluation and
 * dry-run simulation of registered DRL rules.
 *
 * <p>When {@code mode=SIMULATE} the response additionally contains:
 * <ul>
 *   <li>{@code trace} — per-declaration trace entries (one per bound Drools variable).</li>
 *   <li>{@code matchedNodes} — identifiers of declarations / rules that fired.</li>
 *   <li>{@code unmatchedNodes} — identifiers of rules whose matches were cancelled.</li>
 * </ul>
 *
 * <p>All rules referenced by {@code ruleIds} must already be registered via
 * {@code POST /v1/rules} before calling this endpoint.
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/rules")
@Tag(name = "Rule Evaluation", description = "Rule evaluation and simulation API")
public class EvaluateController {

    private static final Logger logger = LoggerFactory.getLogger(EvaluateController.class);

    private final SimulateEvaluationService simulateEvaluationService;

    public EvaluateController(SimulateEvaluationService simulateEvaluationService) {
        this.simulateEvaluationService = simulateEvaluationService;
    }

    @Operation(
            summary = "Evaluate rules (with optional SIMULATE trace)",
            description = "Evaluates one or more registered rules against the provided facts. "
                    + "When mode=SIMULATE the response includes a per-node trace showing which "
                    + "Drools patterns matched, which did not, and the overall verdict.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluation completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request (missing ruleIds or facts)"),
            @ApiResponse(responseCode = "404", description = "One or more ruleIds not registered in engine")
    })
    @PostMapping("/evaluate")
    public ResponseEntity<EvaluateRuleResponse> evaluate(
            @Valid @RequestBody EvaluateRuleRequest request) {

        logger.info("POST /v1/rules/evaluate — mode={}, ruleIds={}", request.getMode(), request.getRuleIds());

        EvaluateRuleResponse response = simulateEvaluationService.evaluate(
                request.getRuleIds(),
                request.getFacts(),
                request.isSimulateMode()
        );

        logger.info("Evaluation completed: verdict={}, traceSize={}",
                response.getVerdict(),
                response.getTrace() != null ? response.getTrace().size() : 0);

        return ResponseEntity.ok(response);
    }
}
