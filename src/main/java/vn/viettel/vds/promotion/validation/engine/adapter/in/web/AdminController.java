package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.engine.application.dto.EngineConfigResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.EngineConfigUpdateRequest;
import vn.viettel.vds.promotion.validation.engine.application.dto.HealthResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.in.ConfigManagementUseCase;
import vn.viettel.vds.promotion.validation.engine.application.port.in.HealthCheckUseCase;

@RestController
@RequestMapping("${spring.application.context-path}/v1")
public class AdminController {

    @Autowired
    private HealthCheckUseCase healthCheckUseCase;

    @Autowired
    private ConfigManagementUseCase configManagementUseCase;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        try {
            HealthResponse health = healthCheckUseCase.getHealth();

            // Return appropriate status code based on health
            if ("UP".equals(health.getStatus())) {
                return ResponseEntity.ok(health);
            } else {
                return ResponseEntity.status(503).body(health); // Service Unavailable
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/configs/{tenantId}")
    public ResponseEntity<EngineConfigResponse> getConfig(@PathVariable String tenantId) {
        try {
            EngineConfigResponse config = configManagementUseCase.getConfig(tenantId);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PatchMapping("/configs/{tenantId}")
    public ResponseEntity<EngineConfigResponse> updateConfig(
            @PathVariable String tenantId,
            @Valid @RequestBody EngineConfigUpdateRequest request) {
        try {
            EngineConfigResponse config = configManagementUseCase.updateConfig(tenantId, request);
            return ResponseEntity.ok(config);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}