package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.EngineConfigEntity;
import vn.viettel.vds.promotion.rule.engine.application.dto.EngineConfigResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.EngineConfigUpdateRequest;
import vn.viettel.vds.promotion.rule.engine.application.port.in.ConfigManagementUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.EngineConfigRepositoryPort;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ConfigManagementService implements ConfigManagementUseCase {

    private final EngineConfigRepositoryPort engineConfigRepository;

    public ConfigManagementService(EngineConfigRepositoryPort engineConfigRepository) {
        this.engineConfigRepository = engineConfigRepository;
    }

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
        if (request.getExecute() != null) {
            updateExecuteConfig(config, request.getExecute());
        }
        if (request.getCompile() != null) {
            updateCompileConfig(config, request.getCompile());
        }
    }

    private void updateExecuteConfig(EngineConfigEntity config, EngineConfigUpdateRequest.ExecuteConfig requestConfig) {
        EngineConfigEntity.ExecuteConfig executeConfig = Optional.ofNullable(config.getExecute()).orElseGet(EngineConfigEntity.ExecuteConfig::new);

        executeConfig.setTimeoutMs(getValue(requestConfig.getTimeoutMs(), executeConfig.getTimeoutMs(), 40));
        executeConfig.setMaxRulesFired(getValue(requestConfig.getMaxRulesFired(), executeConfig.getMaxRulesFired(), 500));
        executeConfig.setMaxFacts(getValue(requestConfig.getMaxFacts(), executeConfig.getMaxFacts(), 500));
        executeConfig.setExplainSampling(getValue(requestConfig.getExplainSampling(), executeConfig.getExplainSampling(), Map.of("FULL", 0.01, "FAIL_ONLY", 1.0)));

        config.setExecute(executeConfig);
    }

    private void updateCompileConfig(EngineConfigEntity config, EngineConfigUpdateRequest.CompileConfig requestConfig) {
        EngineConfigEntity.CompileConfig compileConfig = Optional.ofNullable(config.getCompile()).orElseGet(EngineConfigEntity.CompileConfig::new);

        compileConfig.setMaxNodes(getValue(requestConfig.getMaxNodes(), compileConfig.getMaxNodes(), 400));
        compileConfig.setMaxDepth(getValue(requestConfig.getMaxDepth(), compileConfig.getMaxDepth(), 8));

        config.setCompile(compileConfig);
    }

    private <T> T getValue(T fromRequest, T fromExisting, T defaultValue) {
        return Optional.ofNullable(fromRequest).orElse(Optional.ofNullable(fromExisting).orElse(defaultValue));
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