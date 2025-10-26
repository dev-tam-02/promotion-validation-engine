package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto.ExecuteRequest;
import vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto.ExecuteResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleEnginePort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/execute")
@Tag(name = "Rule Execution", description = "Rule execution API")
public class ExecutionController {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionController.class);

    private final RuleEnginePort ruleEnginePort;

    public ExecutionController(@Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort) {
        this.ruleEnginePort = ruleEnginePort;
    }

    @Operation(summary = "Execute rule", description = "Execute rule against provided context")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Execution failed")
    })
    @PostMapping
    public ResponseEntity<ExecuteResponse> executeRule(@Valid @RequestBody ExecuteRequest request) {

        if (logger.isDebugEnabled()) {
            logger.debug("Executing rule: bundleHash={}, customerId={}",
                    request.getBundleHash(), request.getCustomer().id());
        }

        try {
            // Convert DTO to domain input
            RuleEnginePort.ExecuteInput input = mapToExecuteInput(request);

            // Execute rule
            vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse result = ruleEnginePort.execute(input);

            // Convert to DTO
            ExecuteResponse response = mapToExecuteResponse(result);

            logger.debug("Rule execution completed: decision={}", result.getDecision());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Rule execution failed: bundleHash={}, error={}",
                    request.getBundleHash(), e.getMessage(), e);

            ExecuteResponse errorResponse = new ExecuteResponse();
            errorResponse.setOk(false);
            errorResponse.setDecision("DENY");
            errorResponse.setReasonCodes(List.of("EXECUTION_ERROR"));

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(summary = "Execute rules in batch")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Batch execution failed")
    })
    @PostMapping("/batch")
    public ResponseEntity<List<ExecuteResponse>> executeBatch(
            @Valid @RequestBody List<ExecuteRequest> requests) {

        logger.info("Executing batch: size={}", requests.size());

        try {
            // Convert DTOs to domain inputs
            List<RuleEnginePort.ExecuteInput> inputs = new ArrayList<>();
            for (ExecuteRequest request : requests) {
                inputs.add(mapToExecuteInput(request));
            }

            // Execute batch
            List<vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse> results = ruleEnginePort.executeBatch(inputs);

            // Convert results
            List<ExecuteResponse> responses = new ArrayList<>();
            for (vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse result : results) {
                responses.add(mapToExecuteResponse(result));
            }

            logger.info("Batch execution completed: results={}", results.size());

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Batch execution failed: batchSize={}, error={}",
                    requests.size(), e.getMessage(), e);

            // Create error responses for all requests in batch
            List<ExecuteResponse> errorResponses = new ArrayList<>();
            for (int i = 0; i < requests.size(); i++) {
                ExecuteResponse errorResponse = new ExecuteResponse();
                errorResponse.setOk(false);
                errorResponse.setDecision("DENY");
                errorResponse.setReasonCodes(List.of("EXECUTION_ERROR"));
                errorResponses.add(errorResponse);
            }

            return ResponseEntity.internalServerError().body(errorResponses);
        }
    }

    private RuleEnginePort.ExecuteInput mapToExecuteInput(ExecuteRequest request) {
        // Convert to context map format expected by existing interface
        Map<String, Object> context = new HashMap<>();
        context.put("customer", request.getCustomer());
        context.put("order", request.getOrder());
        context.put("candidate", request.getCandidate());
        context.put("executionContext", request.getExecutionContext());

        return new RuleEnginePort.ExecuteInput(
                request.getExecutionContext().getTenantId(),
                request.getBundleHash(),
                context,
                new RuleEnginePort.ExecuteOptions("NONE", 30000, 1000)
        );
    }

    private ExecuteResponse mapToExecuteResponse(vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse result) {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(result.getOk());
        response.setDecision(result.getDecision());
        response.setReasonCodes(result.getReasonCodes());

        // Convert ExplainEntry list to String list
        List<String> explainStrings = new ArrayList<>();
        if (result.getExplain() != null) {
            for (vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse.ExplainEntry entry : result.getExplain()) {
                explainStrings.add(entry.getNode() + ": " + entry.getOperator() + " = " + entry.getResult());
            }
        }
        response.setExplain(explainStrings);

        // Map engine details
        ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
        if (result.getEngine() != null) {
            engine.setVersion(result.getEngine().getVersion());
            engine.setLatencyMs(result.getEngine().getLatencyMs().longValue());
            engine.setCacheHit(result.getEngine().getCacheHit());
        }
        response.setEngine(engine);

        return response;
    }
}