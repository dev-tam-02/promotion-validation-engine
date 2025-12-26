package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.SupportedOperatorResponse;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.ValidateOperatorsRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.ValidateOperatorsResponse;
import vn.viettel.vds.promotion.rule.engine.application.service.TranslatorDiscoveryService;

import java.util.List;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/operators")
@Tag(name = "Operator Discovery", description = "Operator translator discovery and validation API")
public class OperatorDiscoveryController {

    private static final Logger logger = LoggerFactory.getLogger(OperatorDiscoveryController.class);

    private final TranslatorDiscoveryService translatorDiscoveryService;

    public OperatorDiscoveryController(TranslatorDiscoveryService translatorDiscoveryService) {
        this.translatorDiscoveryService = translatorDiscoveryService;
    }

    @Operation(summary = "Get all supported operators",
            description = "Returns a list of all operators supported by the validation engine with their metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved supported operators"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/supported")
    public ResponseEntity<List<SupportedOperatorResponse>> getSupportedOperators() {
        logger.info("GET /v1/operators/supported - Retrieving all supported operators");

        try {
            List<SupportedOperatorResponse> supportedOperators = translatorDiscoveryService.getSupportedOperators();

            logger.info("Successfully retrieved {} supported operators", supportedOperators.size());
            return ResponseEntity.ok(supportedOperators);

        } catch (Exception e) {
            logger.error("Error retrieving supported operators: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get supported operator names",
            description = "Returns a simple list of supported operator names for quick reference")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved operator names"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/supported/names")
    public ResponseEntity<List<String>> getSupportedOperatorNames() {
        logger.info("GET /v1/operators/supported/names - Retrieving supported operator names");

        try {
            List<String> operatorNames = translatorDiscoveryService.getSupportedOperatorNames();

            logger.info("Successfully retrieved {} operator names", operatorNames.size());
            return ResponseEntity.ok(operatorNames);

        } catch (Exception e) {
            logger.error("Error retrieving operator names: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Validate operators support",
            description = "Validates if given operators are supported by the validation engine")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/validate")
    public ResponseEntity<ValidateOperatorsResponse> validateOperators(
            @Valid @RequestBody ValidateOperatorsRequest request) {

        logger.info("POST /v1/operators/validate - Validating {} operators",
                request.getOperators().size());

        try {
            ValidateOperatorsResponse response = translatorDiscoveryService.validateOperators(request);

            logger.info("Validation completed: {} supported, {} unsupported",
                    response.getSupportedOperators().size(),
                    response.getUnsupportedOperators().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating operators: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Check if specific operator is supported",
            description = "Checks if a specific operator name and version is supported")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/supported/{operatorName}")
    public ResponseEntity<Boolean> isOperatorSupported(
            @Parameter(description = "Operator name to check") @PathVariable String operatorName,
            @Parameter(description = "Operator version (optional)") @RequestParam(required = false) Integer version) {

        logger.info("GET /v1/operators/supported/{} - Checking operator support (version: {})",
                operatorName, version);

        try {
            boolean supported = translatorDiscoveryService.isOperatorSupported(operatorName, version);

            logger.info("Operator {} version {} is {}", operatorName, version,
                    supported ? "supported" : "not supported");

            return ResponseEntity.ok(supported);

        } catch (Exception e) {
            logger.error("Error checking operator support: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}