package vn.viettel.vds.promotion.validation.engine.application.usecase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.jpa.entity.EngineConfigEntity;
import vn.viettel.vds.promotion.validation.engine.application.dto.EngineConfigResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.EngineConfigUpdateRequest;
import vn.viettel.vds.promotion.validation.engine.application.port.in.ConfigManagementUseCase;
import vn.viettel.vds.promotion.validation.engine.application.port.out.EngineConfigRepositoryPort;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ConfigManagementService implements ConfigManagementUseCase {

    @Autowired
    private EngineConfigRepositoryPort engineConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public EngineConfigResponse getConfig(String tenantId) {
        Optional<EngineConfigEntity> config = engineConfigRepository.findByTenantId(tenantId);

        if (config.isPresent()) {
            return mapToConfigResponse(config.get());
        } else {
            // Return default configuration for new tenants
            return getDefaultConfig(tenantId);
        }
    }

    @Override
    public EngineConfigResponse updateConfig(String tenantId, EngineConfigUpdateRequest request) {
        Optional<EngineConfigEntity> existingConfig = engineConfigRepository.findByTenantId(tenantId);

        EngineConfigEntity config;
        if (existingConfig.isPresent()) {
            config = existingConfig.get();
            updateConfigFromRequest(config, request);
            config.setUpdatedAt(Instant.now());
        } else {
            config = createConfigFromRequest(tenantId, request);
        }

        EngineConfigEntity savedConfig = engineConfigRepository.save(config);
        return mapToConfigResponse(savedConfig);
    }

    private EngineConfigResponse getDefaultConfig(String tenantId) {
        EngineConfigResponse response = new EngineConfigResponse();
        response.setTenantId(tenantId);

        // Default execute configuration
        EngineConfigResponse.ExecuteConfig executeConfig = new EngineConfigResponse.ExecuteConfig();
        executeConfig.setTimeoutMs(40);
        executeConfig.setMaxRulesFired(500);
        executeConfig.setMaxFacts(500);
        executeConfig.setExplainSampling(Map.of(
                "FULL", 0.01,
                "FAIL_ONLY", 1.0
        ));
        response.setExecute(executeConfig);

        // Default compile configuration
        EngineConfigResponse.CompileConfig compileConfig = new EngineConfigResponse.CompileConfig();
        compileConfig.setMaxNodes(400);
        compileConfig.setMaxDepth(8);
        response.setCompile(compileConfig);

        Instant now = Instant.now();
        response.setCreatedAt(now);
        response.setUpdatedAt(now);

        return response;
    }

    private EngineConfigEntity createConfigFromRequest(String tenantId, EngineConfigUpdateRequest request) {
        EngineConfigEntity config = new EngineConfigEntity();
        config.setId("cfg_" + tenantId);
        config.setTenantId(tenantId);

        Instant now = Instant.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);

        updateConfigFromRequest(config, request);

        return config;
    }

    private void updateConfigFromRequest(EngineConfigEntity config, EngineConfigUpdateRequest request) {
        // Update execute configuration
        if (request.getExecute() != null) {
            EngineConfigEntity.ExecuteConfig executeConfig = new EngineConfigEntity.ExecuteConfig();

            if (request.getExecute().getTimeoutMs() != null) {
                executeConfig.setTimeoutMs(request.getExecute().getTimeoutMs());
            } else if (config.getExecute() != null) {
                executeConfig.setTimeoutMs(config.getExecute().getTimeoutMs());
            } else {
                executeConfig.setTimeoutMs(40); // Default
            }

            if (request.getExecute().getMaxRulesFired() != null) {
                executeConfig.setMaxRulesFired(request.getExecute().getMaxRulesFired());
            } else if (config.getExecute() != null) {
                executeConfig.setMaxRulesFired(config.getExecute().getMaxRulesFired());
            } else {
                executeConfig.setMaxRulesFired(500); // Default
            }

            if (request.getExecute().getMaxFacts() != null) {
                executeConfig.setMaxFacts(request.getExecute().getMaxFacts());
            } else if (config.getExecute() != null) {
                executeConfig.setMaxFacts(config.getExecute().getMaxFacts());
            } else {
                executeConfig.setMaxFacts(500); // Default
            }

            if (request.getExecute().getExplainSampling() != null) {
                executeConfig.setExplainSampling(request.getExecute().getExplainSampling());
            } else if (config.getExecute() != null) {
                executeConfig.setExplainSampling(config.getExecute().getExplainSampling());
            } else {
                executeConfig.setExplainSampling(Map.of("FULL", 0.01, "FAIL_ONLY", 1.0)); // Default
            }

            config.setExecute(executeConfig);
        }

        // Update compile configuration
        if (request.getCompile() != null) {
            EngineConfigEntity.CompileConfig compileConfig = new EngineConfigEntity.CompileConfig();

            if (request.getCompile().getMaxNodes() != null) {
                compileConfig.setMaxNodes(request.getCompile().getMaxNodes());
            } else if (config.getCompile() != null) {
                compileConfig.setMaxNodes(config.getCompile().getMaxNodes());
            } else {
                compileConfig.setMaxNodes(400); // Default
            }

            if (request.getCompile().getMaxDepth() != null) {
                compileConfig.setMaxDepth(request.getCompile().getMaxDepth());
            } else if (config.getCompile() != null) {
                compileConfig.setMaxDepth(config.getCompile().getMaxDepth());
            } else {
                compileConfig.setMaxDepth(8); // Default
            }

            config.setCompile(compileConfig);
        }
    }

    private EngineConfigResponse mapToConfigResponse(EngineConfigEntity config) {
        EngineConfigResponse response = new EngineConfigResponse();
        response.setTenantId(config.getTenantId());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());

        // Map execute configuration
        if (config.getExecute() != null) {
            EngineConfigResponse.ExecuteConfig executeConfig = new EngineConfigResponse.ExecuteConfig();
            executeConfig.setTimeoutMs(config.getExecute().getTimeoutMs());
            executeConfig.setMaxRulesFired(config.getExecute().getMaxRulesFired());
            executeConfig.setMaxFacts(config.getExecute().getMaxFacts());
            executeConfig.setExplainSampling(config.getExecute().getExplainSampling());
            response.setExecute(executeConfig);
        }

        // Map compile configuration
        if (config.getCompile() != null) {
            EngineConfigResponse.CompileConfig compileConfig = new EngineConfigResponse.CompileConfig();
            compileConfig.setMaxNodes(config.getCompile().getMaxNodes());
            compileConfig.setMaxDepth(config.getCompile().getMaxDepth());
            response.setCompile(compileConfig);
        }

        return response;
    }
}