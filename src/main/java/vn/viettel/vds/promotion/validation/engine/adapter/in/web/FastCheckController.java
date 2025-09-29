package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto.FastCheckRequest;
import vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto.FastCheckResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.in.FastCheckUseCase;

import java.util.List;

/**
 * Fast check controller for simple rule evaluation without Drools.
 * These are deterministic checks that can be evaluated with simple if-else logic.
 *
 * Use this endpoint when you need:
 * - Time-based checks (business hours, blackout periods)
 * - Simple value comparisons (min/max order value)
 * - Blacklist/whitelist checks
 * - Rate limiting checks
 *
 * Performance target: < 5ms response time
 */
@RestController
@RequestMapping("/v1/fast-check")
@Tag(name = "Fast Rule Check", description = "Quick rule evaluation without Drools engine")
@Slf4j
@RequiredArgsConstructor
public class FastCheckController {

    private final FastCheckUseCase fastCheckUseCase;

    @Operation(
        summary = "Fast rule check",
        description = "Performs quick rule evaluation for simple conditions without invoking Drools engine"
    )
    @PostMapping
    public ResponseEntity<FastCheckResponse> fastCheck(@Valid @RequestBody FastCheckRequest request) {

        log.debug("Fast check request for customer={}, order={}",
            request.getCustomerId(), request.getOrderTotal());

        long startTime = System.currentTimeMillis();

        try {
            FastCheckResponse response = fastCheckUseCase.performFastCheck(request);

            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("Fast check completed in {}ms with decision={}", elapsed, response.getDecision());

            // Add performance metrics
            response.setLatencyMs(elapsed);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Fast check failed: {}", e.getMessage(), e);

            // Return DENY on error (fail closed for security)
            return ResponseEntity.ok(FastCheckResponse.deny(
                "FAST_CHECK_ERROR",
                "Fast check evaluation failed: " + e.getMessage()
            ));
        }
    }

    @Operation(
        summary = "Batch fast check",
        description = "Performs fast check for multiple requests in parallel"
    )
    @PostMapping("/batch")
    public ResponseEntity<List<FastCheckResponse>> batchFastCheck(
            @Valid @RequestBody List<FastCheckRequest> requests) {

        log.info("Batch fast check for {} requests", requests.size());

        List<FastCheckResponse> responses = requests.parallelStream()
            .map(request -> {
                try {
                    return fastCheckUseCase.performFastCheck(request);
                } catch (Exception e) {
                    log.error("Fast check failed for request: {}", e.getMessage());
                    return FastCheckResponse.deny("BATCH_ERROR", e.getMessage());
                }
            })
            .toList();

        return ResponseEntity.ok(responses);
    }
}