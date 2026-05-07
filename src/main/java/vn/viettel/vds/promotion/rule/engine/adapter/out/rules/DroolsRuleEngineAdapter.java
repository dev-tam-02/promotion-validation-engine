package vn.viettel.vds.promotion.rule.engine.adapter.out.rules;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ObjectStoragePort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.rule.engine.domain.model.Candidate;
import vn.viettel.vds.promotion.rule.engine.domain.model.Customer;
import vn.viettel.vds.promotion.rule.engine.domain.model.Order;
import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;
import vn.viettel.vds.promotion.rule.engine.domain.service.DroolsCompilationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.RuleTranslationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.TemporalDrlGenerator;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.ExecutionMetricsService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.KieSessionManager;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.RuleExecutionOrchestrator;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Qualifier("droolsRuleEngineAdapter")
public class DroolsRuleEngineAdapter implements RuleEnginePort {

    private static final Logger logger = LoggerFactory.getLogger(DroolsRuleEngineAdapter.class);
    private static final String ERROR_VERSION = "error";

    // Max 500MB total for artifact cache, 60 minutes TTL
    private static final long MAX_ARTIFACT_CACHE_WEIGHT = 500 * 1024 * 1024L;

    private final RuleTranslationService ruleTranslationService;
    private final DroolsCompilationService compilationService;
    private final KieSessionManager sessionManager;
    private final RuleExecutionOrchestrator executionOrchestrator;
    private final ExecutionMetricsService metricsService;
    private final BundleRepositoryPort bundleRepositoryPort;
    private final ObjectStoragePort objectStoragePort;
    private final TemporalDrlGenerator temporalDrlGenerator;

    // Caffeine cache with weight-based eviction (max 100MB) and TTL
    private final Cache<String, byte[]> bundleArtifacts;

    public DroolsRuleEngineAdapter(RuleTranslationService ruleTranslationService,
                                   DroolsCompilationService compilationService,
                                   KieSessionManager sessionManager,
                                   RuleExecutionOrchestrator executionOrchestrator,
                                   ExecutionMetricsService metricsService,
                                   BundleRepositoryPort bundleRepositoryPort,
                                   ObjectStoragePort objectStoragePort,
                                   TemporalDrlGenerator temporalDrlGenerator) {
        this.ruleTranslationService = ruleTranslationService;
        this.compilationService = compilationService;
        this.sessionManager = sessionManager;
        this.executionOrchestrator = executionOrchestrator;
        this.metricsService = metricsService;
        this.bundleRepositoryPort = bundleRepositoryPort;
        this.objectStoragePort = objectStoragePort;
        this.temporalDrlGenerator = temporalDrlGenerator;

        // Initialize Caffeine cache with weight-based eviction (max 500MB) and 60-minute TTL
        this.bundleArtifacts = Caffeine.newBuilder()
                .maximumWeight(MAX_ARTIFACT_CACHE_WEIGHT)
                .weigher((Weigher<String, byte[]>) (key, value) -> value.length)
                .expireAfterAccess(60, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) ->
                        logger.debug("Bundle artifact evicted: key={}, size={}, cause={}", key,
                                value != null ? value.length : 0, cause))
                .build();
    }

    @Override
    public CompileResult compile(CompileInput input) {
        logger.info("Compiling rule: ruleId={}, version={}, hasTemporalData={}",
                input.getRuleId(), input.getVersion(),
                input.getTimeLinks() != null && !input.getTimeLinks().isEmpty());

        long startTime = System.currentTimeMillis();

        try {
            // Check if temporal policy exists
            boolean hasTemporalPolicy = input.getTimeLinks() != null
                    && !input.getTimeLinks().isEmpty()
                    && input.getTimeLinks().get(0).getData() != null;

            // Generate business rule DRL with temporal policy awareness
            String businessRuleDrl = ruleTranslationService.translateToDrl(
                    input.getNodes(),
                    hasTemporalPolicy,
                    input.getRuleId()
            );

            logger.debug("Generated business rule DRL:\n{}", businessRuleDrl);

            DroolsCompilationService.CompilationResult result;
            String combinedDrlContent;

            if (hasTemporalPolicy) {
                logger.info("Temporal policy detected, generating temporal DRL for ruleId={}", input.getRuleId());

                // Get temporal policy data from first timeLink
                TimeLink timeLink = input.getTimeLinks().get(0);
                CompileRequest.TemporalPolicyData temporalData = (CompileRequest.TemporalPolicyData) timeLink.getData();

                // Generate temporal DRL
                String timeframeDrl = temporalDrlGenerator.generateTimeframeDrl(
                        input.getRuleId(),
                        temporalData
                );

                logger.debug("Generated temporal DRL:\n{}", timeframeDrl);

                // Combine 2 DRLs into map
                Map<String, String> drlFiles = new LinkedHashMap<>();
                drlFiles.put("timeframe.drl", timeframeDrl);
                drlFiles.put("validation-rule.drl", businessRuleDrl);

                // Compile multiple DRLs together
                result = compilationService.compileMultipleDrls(
                        input.getRuleId(),
                        input.getVersion(),
                        drlFiles
                );

                // Combine DRL content for storage
                combinedDrlContent = "=== timeframe.drl ===\n" + timeframeDrl + "\n\n=== validation-rule.drl ===\n" + businessRuleDrl;

                logger.info("Successfully compiled 2 DRLs into bundle: bundleHash={}", result.getBundleHash());

            } else {
                logger.info("No temporal policy, compiling business rule only for ruleId={}", input.getRuleId());

                // No temporal policy - compile business rule only (existing behavior)
                result = compilationService.compileDrl(
                        input.getRuleId(),
                        input.getVersion(),
                        businessRuleDrl
                );

                combinedDrlContent = businessRuleDrl;
            }

            bundleArtifacts.put(result.getBundleHash(), result.getArtifactBytes());

            // Record compilation metrics
            metricsService.recordCompilation(Duration.ofMillis(System.currentTimeMillis() - startTime), true);

            return new CompileResult(
                    result.getBundleHash(),
                    result.getArtifactBytes(),
                    result.getSize(),
                    result.getLogs(),
                    result.getDroolsVersion(),
                    combinedDrlContent
            );

        } catch (Exception e) {
            // Record compilation failure metrics
            metricsService.recordCompilation(Duration.ofMillis(System.currentTimeMillis() - startTime), false);

            String errorMessage = String.format("Compilation failed for rule '%s' (version: %s): %s",
                    input.getRuleId(), input.getVersion(),
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            throw new RuleBundleException(errorMessage, e);
        }
    }

    @Override
    public ExecuteResponse execute(ExecuteInput input) {
        logger.debug("Executing rule: bundleHash={}", input.getBundleHash());

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
                    logger.warn("Failed to get container for bundle: {}", bundleHash, e);
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
            throw new RuleBundleException(
                    String.format("Bundle warmup failed for hash '%s'", bundleHash), e);
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
        // 1. Check if container is already in session cache
        KieContainer container = sessionManager.getCachedContainer(bundleHash);
        if (container != null) {
            logger.debug("Container found in session cache: bundleHash={}", bundleHash);
            return container;
        }

        // 2. Check if artifact bytes are in memory cache
        byte[] artifactBytes = bundleArtifacts.getIfPresent(bundleHash);
        if (artifactBytes != null) {
            logger.debug("Artifact found in memory cache: bundleHash={}", bundleHash);
            container = compilationService.createKieContainer(artifactBytes);
            sessionManager.cacheContainer(bundleHash, container);
            return container;
        }

        // 3. Try to load bundle from database and object storage
        logger.info("Bundle not in cache, attempting to load from database: bundleHash={}", bundleHash);
        try {
            artifactBytes = loadBundleFromPersistence(bundleHash);
            if (artifactBytes != null) {
                logger.info("Bundle loaded from persistence: bundleHash={}, size={}",
                        bundleHash, artifactBytes.length);

                // Cache the artifact bytes for future use
                bundleArtifacts.put(bundleHash, artifactBytes);

                // Create and cache the container
                container = compilationService.createKieContainer(artifactBytes);
                sessionManager.cacheContainer(bundleHash, container);
                return container;
            }
        } catch (Exception e) {
            logger.error("Failed to load bundle from persistence: bundleHash={}, error={}",
                    bundleHash, e.getMessage(), e);
        }

        // 4. Bundle not found anywhere
        throw new IllegalStateException("Bundle not found: " + bundleHash +
                ". The bundle must be compiled and stored before execution.");
    }

    /**
     * Load bundle artifact bytes from database and object storage
     *
     * @param bundleHash the bundle hash identifier
     * @return artifact bytes or null if not found
     */
    @SuppressWarnings("java:S2139")
    private byte[] loadBundleFromPersistence(String bundleHash) {
        // Find bundle entity in database
        Optional<BundleEntity> bundleOpt = bundleRepositoryPort.findById(bundleHash);
        if (bundleOpt.isEmpty()) {
            throw new RuleBundleException(
                    String.format("Bundle not found in database: bundleHash=%s", bundleHash));
        }

        BundleEntity bundle = bundleOpt.get();
        logger.debug("Found bundle in database: bundleHash={}, ruleId={}, version={}",
                bundleHash, bundle.getRuleId(), bundle.getRuleVersion());

        // Try to load artifact bytes from object storage
        if (bundle.getArtifact() != null && bundle.getArtifact().getKey() != null) {
            String artifactKey = bundle.getArtifact().getKey();
            logger.debug("Retrieving artifact from storage: key={}", artifactKey);

            Optional<byte[]> artifactBytesOpt = objectStoragePort.retrieve(artifactKey);
            if (artifactBytesOpt.isPresent()) {
                logger.info("Artifact retrieved from storage: key={}, size={}",
                        artifactKey, artifactBytesOpt.get().length);
                return artifactBytesOpt.get();
            } else {
                logger.warn("Artifact not found in storage: key={}", artifactKey);
            }
        }

        // Fallback: compile from DRL content if artifact not available
        if (bundle.getDrlContent() != null && !bundle.getDrlContent().isEmpty()) {
            logger.info("Artifact not available, compiling from DRL content: bundleHash={}", bundleHash);
            try {
                DroolsCompilationService.CompilationResult result = compilationService.compileDrl(
                        bundle.getRuleId(),
                        bundle.getRuleVersion(),
                        bundle.getDrlContent()
                );
                logger.info("DRL compiled successfully: bundleHash={}, size={}",
                        bundleHash, result.getArtifactBytes().length);
                return result.getArtifactBytes();
            } catch (Exception e) {
                throw new RuleBundleException(
                        String.format("Failed to compile DRL for bundle: bundleHash=%s", bundleHash), e);
            }
        }

        throw new RuleBundleException(
                String.format("No artifact or DRL content available for bundle: bundleHash=%s", bundleHash));
    }

    // Legacy methods - delegate to existing RuleEngineAdapter
    @Override
    public ValidationResult executeRules(Customer customer, Order order, Candidate candidate, String bundleHash) {
        Map<String, Object> context = new HashMap<>();
        context.put("customer", customer);
        context.put("order", order);
        context.put("candidate", candidate);

        ExecuteInput input = new ExecuteInput(
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