package vn.viettel.vds.promotion.validation.engine.adapter.out.rules;

import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.validation.engine.domain.model.Candidate;
import vn.viettel.vds.promotion.validation.engine.domain.model.Customer;
import vn.viettel.vds.promotion.validation.engine.domain.model.Order;
import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;
import vn.viettel.vds.promotion.validation.engine.domain.service.DroolsCompilationService;
import vn.viettel.vds.promotion.validation.engine.domain.service.RuleTranslationService;
import vn.viettel.vds.promotion.validation.engine.domain.service.execution.ExecutionMetricsService;
import vn.viettel.vds.promotion.validation.engine.domain.service.execution.KieSessionManager;
import vn.viettel.vds.promotion.validation.engine.domain.service.execution.RuleExecutionOrchestrator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Qualifier("droolsRuleEngineAdapter")
public class DroolsRuleEngineAdapter implements RuleEnginePort {

    private static final Logger logger = LoggerFactory.getLogger(DroolsRuleEngineAdapter.class);
    private static final String ERROR_VERSION = "error";

    private final RuleTranslationService ruleTranslationService;
    private final DroolsCompilationService compilationService;
    private final KieSessionManager sessionManager;
    private final RuleExecutionOrchestrator executionOrchestrator;
    private final ExecutionMetricsService metricsService;

    private final ConcurrentHashMap<String, byte[]> bundleArtifacts = new ConcurrentHashMap<>();

    public DroolsRuleEngineAdapter(RuleTranslationService ruleTranslationService,
                                   DroolsCompilationService compilationService,
                                   KieSessionManager sessionManager,
                                   RuleExecutionOrchestrator executionOrchestrator,
                                   ExecutionMetricsService metricsService) {
        this.ruleTranslationService = ruleTranslationService;
        this.compilationService = compilationService;
        this.sessionManager = sessionManager;
        this.executionOrchestrator = executionOrchestrator;
        this.metricsService = metricsService;
    }

    @Override
    public CompileResult compile(CompileInput input) {
        logger.info("Compiling rule: tenantId={}, ruleId={}, version={}",
                input.getTenantId(), input.getRuleId(), input.getVersion());

        long startTime = System.currentTimeMillis();

        try {
            String drlContent = ruleTranslationService.translateToDrl(
                    input.getTenantId(),
                    input.getRuleId(),
                    input.getVersion(),
                    input.getNodes()
            );

            logger.debug("Generated DRL content:\n{}", drlContent);

            DroolsCompilationService.CompilationResult result = compilationService.compileDrl(
                    input.getTenantId(),
                    input.getRuleId(),
                    input.getVersion(),
                    drlContent
            );

            bundleArtifacts.put(result.getBundleHash(), result.getArtifactBytes());

            // Record compilation metrics
            metricsService.recordCompilation(Duration.ofMillis(System.currentTimeMillis() - startTime), true);

            return new CompileResult(
                    result.getBundleHash(),
                    result.getArtifactBytes(),
                    result.getSize(),
                    result.getLogs(),
                    result.getDroolsVersion()
            );

        } catch (Exception e) {
            String errorMessage = String.format("Compilation failed for rule '%s' (tenant: %s, version: %s): %s",
                    input.getRuleId(), input.getTenantId(), input.getVersion(), e.getMessage());
            logger.error(errorMessage, e);

            // Record compilation failure metrics
            metricsService.recordCompilation(Duration.ofMillis(System.currentTimeMillis() - startTime), false);

            throw new RuleBundleException(errorMessage, e);
        }
    }

    @Override
    public ExecuteResponse execute(ExecuteInput input) {
        logger.debug("Executing rule: bundleHash={}, tenantId={}",
                input.getBundleHash(), input.getTenantId());

        try {
            KieContainer container = getOrCreateContainer(input.getBundleHash());
            return executionOrchestrator.executeSingle(input, container);
        } catch (Exception e) {
            logger.error("Rule execution failed: bundleHash={}, error={}",
                    input.getBundleHash(), e.getMessage(), e);

            ExecuteResponse response = new ExecuteResponse();
            response.setOk(false);
            response.setDecision("DENY");
            response.setReasonCodes(List.of("EXECUTION_ERROR"));
            response.setExplain(List.of(
                    new ExecuteResponse.ExplainEntry(ERROR_VERSION, "exception", false)
            ));

            ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
            engine.setVersion(ERROR_VERSION);
            engine.setLatencyMs(0);
            engine.setCacheHit(false);
            response.setEngine(engine);

            return response;
        }
    }

    @Override
    public List<ExecuteResponse> executeBatch(List<ExecuteInput> inputs) {
        logger.info("Executing batch: size={}", inputs.size());

        try {
            Map<String, KieContainer> containersByBundle = loadContainersForBatch(inputs);
            return executionOrchestrator.executeOptimizedBatch(inputs, containersByBundle);
        } catch (Exception e) {
            logger.error("Batch execution failed", e);
            return createBatchErrorResponses(inputs.size());
        }
    }

    private Map<String, KieContainer> loadContainersForBatch(List<ExecuteInput> inputs) {
        Map<String, KieContainer> containersByBundle = new HashMap<>();

        for (ExecuteInput input : inputs) {
            String bundleHash = input.getBundleHash();
            if (!containersByBundle.containsKey(bundleHash)) {
                try {
                    KieContainer container = getOrCreateContainer(bundleHash);
                    containersByBundle.put(bundleHash, container);
                } catch (Exception e) {
                    logger.warn("Failed to get container for bundle: {}, error: {}", bundleHash, e.getMessage());
                    // Container will be null, handled in orchestrator
                }
            }
        }

        return containersByBundle;
    }

    private List<ExecuteResponse> createBatchErrorResponses(int count) {
        List<ExecuteResponse> errorResponses = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ExecuteResponse response = new ExecuteResponse();
            response.setOk(false);
            response.setDecision("DENY");
            response.setReasonCodes(List.of("BATCH_EXECUTION_ERROR"));
            response.setExplain(List.of(
                    new ExecuteResponse.ExplainEntry("batch", ERROR_VERSION, false)
            ));

            ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
            engine.setVersion(ERROR_VERSION);
            engine.setLatencyMs(0);
            engine.setCacheHit(false);
            response.setEngine(engine);

            errorResponses.add(response);
        }

        return errorResponses;
    }

    @Override
    public void warmupBundle(String bundleHash, byte[] artifactBytes) {
        logger.info("Warming up bundle: bundleHash={}", bundleHash);

        try {
            bundleArtifacts.put(bundleHash, artifactBytes);
            KieContainer container = compilationService.createKieContainer(artifactBytes);
            sessionManager.cacheContainer(bundleHash, container);

            logger.info("Bundle warmup completed: bundleHash={}", bundleHash);
        } catch (Exception e) {
            String errorMessage = String.format("Bundle warmup failed for hash '%s': %s",
                    bundleHash, e.getMessage());
            logger.error(errorMessage, e);
            throw new RuleBundleException(errorMessage, e);
        }
    }

    @Override
    public boolean isRuleBundleLoaded(String bundleHash) {
        return sessionManager.isContainerCached(bundleHash);
    }

    @Override
    public boolean isBundleCached(String bundleHash) {
        return isRuleBundleLoaded(bundleHash);
    }

    private KieContainer getOrCreateContainer(String bundleHash) {
        KieContainer container = sessionManager.getCachedContainer(bundleHash);
        if (container != null) {
            return container;
        }

        byte[] artifactBytes = bundleArtifacts.get(bundleHash);
        if (artifactBytes == null) {
            throw new IllegalStateException("Bundle not found: " + bundleHash +
                    ". Make sure to warm up the bundle before execution.");
        }

        container = compilationService.createKieContainer(artifactBytes);
        sessionManager.cacheContainer(bundleHash, container);

        return container;
    }

    // Legacy methods - delegate to existing RuleEngineAdapter
    @Override
    public ValidationResult executeRules(Customer customer, Order order, Candidate candidate, String bundleHash) {
        Map<String, Object> context = new HashMap<>();
        context.put("customer", customer);
        context.put("order", order);
        context.put("candidate", candidate);

        ExecuteInput input = new ExecuteInput(
                "default",
                bundleHash,
                context,
                new ExecuteOptions("NONE", 30000, 1000)
        );

        ExecuteResponse response = execute(input);

        ValidationResult result = new ValidationResult();
        result.setOk(response.getOk());
        result.setDecision(response.getDecision());
        result.setReasonCodes(response.getReasonCodes());
        List<String> explainStrings = response.getExplain().stream()
                .map(entry -> entry.getNode() + ": " + entry.getOperator() + " = " + entry.getResult())
                .toList();
        result.setMessage(String.join("; ", explainStrings));

        return result;
    }

    @Override
    public List<ValidationResult> executeBulkRules(Customer customer, Order order, List<Candidate> candidates) {
        List<ValidationResult> results = new ArrayList<>();

        for (Candidate candidate : candidates) {
            String bundleHash = determineBundleHash(candidate);
            ValidationResult result = executeRules(customer, order, candidate, bundleHash);
            results.add(result);
        }

        return results;
    }

    @Override
    public void loadRuleBundle(String bundleHash, byte[] kieModuleBytes) {
        warmupBundle(bundleHash, kieModuleBytes);
    }

    private String determineBundleHash(Candidate candidate) {
        return candidate.getType() + "-bundle";
    }
}