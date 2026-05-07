package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.DrlErrorResponse;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.RegisterRuleRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.RegisterRuleResponse;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.UpdateRuleRequest;
import vn.viettel.vds.promotion.rule.engine.application.port.in.RegisterDrlUseCase;

/**
 * REST controller for DRL rule CRUD operations.
 *
 * <p>Exposes POST/PUT/DELETE /v1/rules so that pp-validation's authoring flow can register
 * compiled DRL rules directly in pp-rule-engine. This is the primary integration point for
 * Task 07 (pp-validation → pp-rule-engine DRL registration).
 *
 * <p>Path prefix comes from {@code spring.application.context-path} property
 * ({@code /promotion/promotion-rule-engine}).
 */
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/rules")
@Tag(name = "Rule Registry", description = "DRL rule registration CRUD")
public class RulesController {

    private static final Logger logger = LoggerFactory.getLogger(RulesController.class);

    private final RegisterDrlUseCase registerDrlUseCase;

    public RulesController(RegisterDrlUseCase registerDrlUseCase) {
        this.registerDrlUseCase = registerDrlUseCase;
    }

    // ------------------------------------------------------------------
    // POST /v1/rules  — register a new DRL rule
    // ------------------------------------------------------------------

    @Operation(
            summary = "Register a DRL rule",
            description = "Accepts raw DRL content and registers it in the engine. "
                    + "Idempotent: same (id, drl) returns the existing bundleHash. "
                    + "Returns 409 if the id exists with different DRL — use PUT to update.")
    @ApiResponse(responseCode = "201", description = "Rule registered successfully")
    @ApiResponse(responseCode = "400", description = "DRL syntax error")
    @ApiResponse(responseCode = "409", description = "Rule id already exists with different DRL")
    @PostMapping
    public ResponseEntity<Object> registerRule(@Valid @RequestBody RegisterRuleRequest request) {
        logger.info("POST /v1/rules — ruleId={}", request.getId());

        try {
            RegisterDrlUseCase.RegisterRuleResult result =
                    registerDrlUseCase.register(request.getId(), request.getDrl());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegisterRuleResponse(result.ruleId(), result.bundleHash()));

        } catch (RegisterDrlUseCase.DrlCompileException e) {
            logger.warn("DRL compile error for ruleId={}: {}", request.getId(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new DrlErrorResponse("DRL_COMPILE_ERROR", e.getMessage(), e.getCompileLogs()));

        } catch (RegisterDrlUseCase.DrlConflictException e) {
            logger.warn("DRL conflict for ruleId={}: {}", request.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new DrlErrorResponse("RULE_CONFLICT", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // PUT /v1/rules/{id}  — update an existing DRL rule
    // ------------------------------------------------------------------

    @Operation(
            summary = "Update a registered DRL rule",
            description = "Replaces the DRL content of an existing rule and recompiles. "
                    + "Returns 404 if the rule id is not found.")
    @ApiResponse(responseCode = "200", description = "Rule updated successfully")
    @ApiResponse(responseCode = "400", description = "DRL syntax error")
    @ApiResponse(responseCode = "404", description = "Rule not found")
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateRule(@PathVariable String id,
                                             @Valid @RequestBody UpdateRuleRequest request) {
        logger.info("PUT /v1/rules/{} — updating DRL", id);

        try {
            RegisterDrlUseCase.RegisterRuleResult result =
                    registerDrlUseCase.update(id, request.getDrl());

            return ResponseEntity.ok(new RegisterRuleResponse(result.ruleId(), result.bundleHash()));

        } catch (RegisterDrlUseCase.RuleNotFoundException e) {
            logger.warn("Rule not found for PUT: ruleId={}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new DrlErrorResponse("RULE_NOT_FOUND", e.getMessage()));

        } catch (RegisterDrlUseCase.DrlCompileException e) {
            logger.warn("DRL compile error on PUT for ruleId={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new DrlErrorResponse("DRL_COMPILE_ERROR", e.getMessage(), e.getCompileLogs()));
        }
    }

    // ------------------------------------------------------------------
    // DELETE /v1/rules/{id}  — remove a registered rule
    // ------------------------------------------------------------------

    @Operation(
            summary = "Delete a registered DRL rule",
            description = "Removes the rule from the registry. Returns 404 if not found.")
    @ApiResponse(responseCode = "204", description = "Rule deleted")
    @ApiResponse(responseCode = "404", description = "Rule not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable String id) {
        logger.info("DELETE /v1/rules/{}", id);

        try {
            registerDrlUseCase.delete(id);
            return ResponseEntity.noContent().build();

        } catch (RegisterDrlUseCase.RuleNotFoundException e) {
            logger.warn("Rule not found for DELETE: ruleId={}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
