package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.EngineConfigEntity;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteRequest;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.in.ExecutionUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.EngineConfigRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ObjectStoragePort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional(readOnly = true)
public class ExecutionService implements ExecutionUseCase {

    private static final String BUNDLE_NOT_FOUND_MSG = "Bundle not found: ";

    private final BundleRepositoryPort bundleRepository;
    private final EngineConfigRepositoryPort engineConfigRepository;
    private final RuleEnginePort ruleEnginePort;
    private final ObjectStoragePort objectStoragePort;

    public ExecutionService(
            BundleRepositoryPort bundleRepository,
            EngineConfigRepositoryPort engineConfigRepository,
            @Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort,
            ObjectStoragePort objectStoragePort) {
        this.bundleRepository = bundleRepository;
        this.engineConfigRepository = engineConfigRepository;
        this.ruleEnginePort = ruleEnginePort;
        this.objectStoragePort = objectStoragePort;
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        String bundleHash = request.getBundle().getHash();
        validateBundleExists(bundleHash);

        Optional<BundleEntity> bundle = bundleRepository.findById(bundleHash);

        // Get tenant configuration
        Optional<EngineConfigEntity> config = engineConfigRepository.findByTenantId(request.getTenantId());
        ExecuteRequest.ExecuteOptions effectiveOptions = mergeOptions(request.getOptions(), config.orElse(null));

        // Ensure bundle is warmed up
        if (bundle.isPresent()) {
            ensureBundleWarmedUp(bundle.get());
        }

        // Execute rules
        RuleEnginePort.ExecuteInput executeInput = new RuleEnginePort.ExecuteInput(
                request.getTenantId(),
                bundleHash,
                request.getContext(),
                mapToRuleEngineOptions(effectiveOptions)
        );

        try {
            return ruleEnginePort.execute(executeInput);
        } catch (Exception e) {
            throw new RuleExecutionException("Rule execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public BatchExecuteResponse executeBatch(BatchExecuteRequest request) {
        String bundleHash = request.getBundle().getHash();
        validateBundleExists(bundleHash);

        Optional<BundleEntity> bundle = bundleRepository.findById(bundleHash);

        // Get tenant configuration
        Optional<EngineConfigEntity> config = engineConfigRepository.findByTenantId(request.getTenantId());

        // Ensure bundle is warmed up
        if (bundle.isPresent()) {
            ensureBundleWarmedUp(bundle.get());
        }

        // Prepare execution inputs
        List<RuleEnginePort.ExecuteInput> executeInputs = request.getCases().stream()
                .map(testCase -> {
                    validateContext(testCase.getContext());
                    return new RuleEnginePort.ExecuteInput(
                            request.getTenantId(),
                            bundleHash,
                            testCase.getContext(),
                            getDefaultOptions(config.orElse(null))
                    );
                })
                .toList();

        // Execute batch
        long startTime = System.currentTimeMillis();
        List<ExecuteResponse> responses = ruleEnginePort.executeBatch(executeInputs);
        long totalTime = System.currentTimeMillis() - startTime;

        // Prepare results
        List<BatchExecuteResponse.TestResult> results = new ArrayList<>();
        AtomicInteger passCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < request.getCases().size(); i++) {
            BatchExecuteRequest.TestCase testCase = request.getCases().get(i);
            ExecuteResponse response = responses.get(i);

            results.add(new BatchExecuteResponse.TestResult(testCase.getName(), response));

            if (Boolean.TRUE.equals(response.getOk()) && "ALLOW".equals(response.getDecision())) {
                passCount.incrementAndGet();
            } else {
                failCount.incrementAndGet();
            }
        }

        BatchExecuteResponse.Stats stats = new BatchExecuteResponse.Stats(
                passCount.get(), failCount.get(), totalTime);

        return new BatchExecuteResponse(results, stats);
    }

    private ExecuteRequest.ExecuteOptions mergeOptions(ExecuteRequest.ExecuteOptions requestOptions,
                                                       EngineConfigEntity config) {
        ExecuteRequest.ExecuteOptions merged = new ExecuteRequest.ExecuteOptions();

        if (requestOptions != null) {
            merged.setExplain(requestOptions.getExplain());
            merged.setTimeoutMs(requestOptions.getTimeoutMs());
            merged.setMaxRulesFired(requestOptions.getMaxRulesFired());
        }

        // Apply defaults from config
        if (config != null && config.getExecute() != null) {
            EngineConfigEntity.ExecuteConfig execConfig = config.getExecute();

            if (merged.getTimeoutMs() == null) {
                merged.setTimeoutMs(execConfig.getTimeoutMs());
            }
            if (merged.getMaxRulesFired() == null) {
                merged.setMaxRulesFired(execConfig.getMaxRulesFired());
            }
            if (merged.getExplain() == null) {
                merged.setExplain("FAIL_ONLY"); // Default explain level
            }
        }

        // Apply system defaults
        if (merged.getTimeoutMs() == null) {
            merged.setTimeoutMs(40); // 40ms default
        }
        if (merged.getMaxRulesFired() == null) {
            merged.setMaxRulesFired(500);
        }
        if (merged.getExplain() == null) {
            merged.setExplain("NONE");
        }

        return merged;
    }

    private RuleEnginePort.ExecuteOptions mapToRuleEngineOptions(ExecuteRequest.ExecuteOptions options) {
        return new RuleEnginePort.ExecuteOptions(
                options.getExplain(),
                options.getTimeoutMs(),
                options.getMaxRulesFired()
        );
    }

    private RuleEnginePort.ExecuteOptions getDefaultOptions(EngineConfigEntity config) {
        ExecuteRequest.ExecuteOptions defaultOptions = mergeOptions(null, config);
        return mapToRuleEngineOptions(defaultOptions);
    }

    private void validateContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            throw new IllegalArgumentException("Context cannot be null or empty");
        }

        // Basic validation - ensure required fields exist
        if (!context.containsKey("now")) {
            throw new IllegalArgumentException("Context must contain 'now' field");
        }

        // Future enhancement: Add more sophisticated schema validation
        // This could include:
        // 1. JSON Schema validation
        // 2. Type checking for known fields
        // 3. Required field validation
        // - Validate customer structure
        // - Validate order structure
        // - Validate items array
        // - Validate metadata structure
    }

    private void ensureBundleWarmedUp(BundleEntity bundle) {
        String bundleHash = bundle.getId();

        // Check if already cached
        if (ruleEnginePort.isBundleCached(bundleHash)) {
            return;
        }

        // Retrieve artifact and warm up
        String artifactKey = bundle.getArtifact().getKey();
        Optional<byte[]> artifactBytes = objectStoragePort.retrieve(artifactKey);

        if (artifactBytes.isEmpty()) {
            throw new IllegalStateException("Bundle artifact not found in storage: " + artifactKey);
        }

        ruleEnginePort.warmupBundle(bundleHash, artifactBytes.get());
    }

    private void validateBundleExists(String bundleHash) {
        Optional<BundleEntity> bundle = bundleRepository.findById(bundleHash);
        if (bundle.isEmpty()) {
            throw new IllegalArgumentException(BUNDLE_NOT_FOUND_MSG + bundleHash);
        }
    }
}