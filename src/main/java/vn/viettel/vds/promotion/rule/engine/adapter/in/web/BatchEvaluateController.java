package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.BatchEvaluateRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.BatchEvaluateResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.in.BatchEvaluateUseCase;

/**
 * Single-hop evaluate entry point consumed by pp-redemption.
 * <p>
 * Replaces the prior multi-call dance (resolve bundleHash in
 * pp-validation → execute via pp-rule-engine) with one call that
 * owns the full resolve → fast-check → execute pipeline.
 */
@RestController
@ResponseWrapper
@RequiredArgsConstructor
@RequestMapping("${spring.application.context-path}/v1")
@Tag(name = "Batch Evaluate", description = "Single-hop rule evaluation for redemption")
public class BatchEvaluateController {

    private final BatchEvaluateUseCase batchEvaluateUseCase;

    @Operation(summary = "Evaluate many subjects in a single hop",
            description = "Resolves the bundle, runs fast-check and Drools execution for each subject, "
                    + "and returns one decision per subject. Fail-closed on missing rules.")
    @PostMapping("/batch-evaluate")
    public BatchEvaluateResponse batchEvaluate(@Valid @RequestBody BatchEvaluateRequest request) {
        return batchEvaluateUseCase.evaluate(request);
    }
}
