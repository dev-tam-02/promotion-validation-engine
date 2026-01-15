package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.rule.engine.application.dto.EngineConfigResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.EngineConfigUpdateRequest;
import vn.viettel.vds.promotion.rule.engine.application.dto.HealthResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.in.ConfigManagementUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.in.HealthCheckUseCase;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1")
public class AdminController {

    private final HealthCheckUseCase healthCheckUseCase;
    private final ConfigManagementUseCase configManagementUseCase;

    public AdminController(HealthCheckUseCase healthCheckUseCase, ConfigManagementUseCase configManagementUseCase) {
        this.healthCheckUseCase = healthCheckUseCase;
        this.configManagementUseCase = configManagementUseCase;
    }

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

    @GetMapping("/configs")
    public ResponseEntity<EngineConfigResponse> getConfig() {
        try {
            EngineConfigResponse config = configManagementUseCase.getConfig();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PatchMapping("/configs")
    public ResponseEntity<EngineConfigResponse> updateConfig(
            @Valid @RequestBody EngineConfigUpdateRequest request) {
        try {
            EngineConfigResponse config = configManagementUseCase.updateConfig(request);
            return ResponseEntity.ok(config);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}