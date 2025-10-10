package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto.*;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleEnginePort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/compile")
@Tag(name = "Rule Compilation", description = "Rule compilation API")
public class CompilationController {

    private static final Logger logger = LoggerFactory.getLogger(CompilationController.class);

    private final RuleEnginePort ruleEnginePort;

    public CompilationController(@Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort) {
        this.ruleEnginePort = ruleEnginePort;
    }

    @Operation(summary = "Compile rule to Drools artifact",
            description = "Compile rule nodes into executable Drools bundle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule compiled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Compilation failed")
    })
    @PostMapping
    public ResponseEntity<CompileResponse> compileRule(@Valid @RequestBody CompileRequest request) {

        logger.info("Compiling rule: tenantId={}, ruleId={}, version={}",
                request.getTenantId(), request.getRuleId(), request.getVersion());

        try {
            // Convert DTO to domain input
            RuleEnginePort.CompileInput input = mapToCompileInput(request);

            // Compile rule
            RuleEnginePort.CompileResult result = ruleEnginePort.compile(input);

            // Convert result to DTO
            CompileResponse response = mapToCompileResponse(result);

            logger.info("Rule compilation completed: bundleHash={}, ok={}",
                    response.getBundleHash(), response.isOk());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Rule compilation failed: tenantId={}, ruleId={}, error={}",
                    request.getTenantId(), request.getRuleId(), e.getMessage(), e);

            CompileResponse errorResponse = new CompileResponse();
            errorResponse.setOk(false);
            errorResponse.setErrors(List.of("Compilation failed: " + e.getMessage()));

            return ResponseEntity.ok(errorResponse);
        }
    }

    @Operation(summary = "Warm up compiled bundle")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bundle warmed up successfully"),
            @ApiResponse(responseCode = "500", description = "Warmup failed")
    })
    @PostMapping("/warmup")
    public ResponseEntity<Void> warmupBundle(@Valid @RequestBody WarmupRequest request) {

        logger.info("Warming up bundle: bundleHash={}", request.getBundleHash());

        try {
            ruleEnginePort.warmupBundle(request.getBundleHash(), request.getArtifactBytes());

            logger.info("Bundle warmup completed: bundleHash={}", request.getBundleHash());

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Bundle warmup failed: bundleHash={}, error={}",
                    request.getBundleHash(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get bundle status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bundle status retrieved"),
            @ApiResponse(responseCode = "404", description = "Bundle not found"),
            @ApiResponse(responseCode = "500", description = "Status check failed")
    })
    @GetMapping("/bundle/{bundleHash}/status")
    public ResponseEntity<BundleStatusResponse> getBundleStatus(
            @Parameter(description = "Bundle hash") @PathVariable String bundleHash) {

        logger.debug("Getting bundle status: bundleHash={}", bundleHash);

        try {
            boolean isLoaded = ruleEnginePort.isRuleBundleLoaded(bundleHash);

            BundleStatusResponse response = new BundleStatusResponse();
            response.setBundleHash(bundleHash);
            response.setLoaded(isLoaded);
            response.setHealth(isLoaded ? "HEALTHY" : "NOT_LOADED");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Bundle status check failed: bundleHash={}, error={}",
                    bundleHash, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private RuleEnginePort.CompileInput mapToCompileInput(CompileRequest request) {
        // Convert DTOs to Map format expected by existing interface
        List<Map<String, Object>> nodesMaps = new ArrayList<>();
        if (request.getNodes() != null) {
            for (RuleNodeDto node : request.getNodes()) {
                Map<String, Object> nodeMap = new HashMap<>();
                nodeMap.put("id", node.getId());
                nodeMap.put("type", node.getType());
                nodeMap.put("groupLogic", node.getGroupLogic());
                nodeMap.put("operatorName", node.getOperatorName());
                nodeMap.put("operatorVersion", node.getOperatorVersion());
                nodeMap.put("params", node.getParams());
                nodeMap.put("reasonCode", node.getReasonCode());
                nodeMap.put("children", node.getChildren());
                nodeMap.put("order", node.getOrder());
                nodesMaps.add(nodeMap);
            }
        }

        return new RuleEnginePort.CompileInput(
                request.getTenantId(),
                request.getRuleId(),
                request.getVersion(),
                nodesMaps,
                request.getOperatorsFingerprint(),
                "default-compiler"
        );
    }

    private CompileResponse mapToCompileResponse(RuleEnginePort.CompileResult result) {
        CompileResponse response = new CompileResponse();
        response.setOk(true);
        response.setBundleHash(result.getBundleHash());
        response.setArtifactBytes(result.getArtifactBytes());
        response.setArtifactSize(result.getSize());
        response.setLogs(result.getLogs() != null ? result.getLogs() : new ArrayList<>());
        response.setEngineVersion(result.getDroolsVersion());
        response.setErrors(new ArrayList<>()); // Initialize errors as empty list for successful compilation
        return response;
    }
}