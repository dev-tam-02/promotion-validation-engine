package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class RuleExecutionOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(RuleExecutionOrchestrator.class);
    private static final int DEFAULT_TIMEOUT_MS = 30000; // 30 seconds default timeout
    private static final int MIN_TIMEOUT_MS = 100; // Minimum timeout to prevent misuse
    private static final String EXECUTION_ERROR = "error";

    private final KieSessionManager sessionManager;
    private final FactPreparationService factPreparationService;
    private final ExecutionTracingService tracingService;
    private final ExecutionMetricsService metricsService;
    private final Executor executionExecutor;

    public RuleExecutionOrchestrator(KieSessionManager sessionManager,
                                     FactPreparationService factPreparationService,
                                     ExecutionTracingService tracingService,
                                     ExecutionMetricsService metricsService) {
        this.sessionManager = sessionManager;
        this.factPreparationService = factPreparationService;
        this.tracingService = tracingService;
        this.metricsService = metricsService;
        this.executionExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public ExecuteResponse executeSingle(RuleEnginePort.ExecuteInput input, KieContainer container) {
        String executionId = generateExecutionId();
        logger.info("Starting single rule execution: executionId={}, bundleHash={}",
                executionId, input.getBundleHash());

        long startTime = System.currentTimeMillis();
        ExecuteResponse response = new ExecuteResponse();

        try {
            // Create stateless session
            StatelessKieSession session = sessionManager.createStatelessSession(container);

            // Set up tracing if explain is enabled
            ExecutionTracingService.TracingAgendaEventListener tracingListener = null;
            if (shouldEnableExplain(input.getOptions())) {
                tracingListener = tracingService.createTracingListener(executionId);
                session.addEventListener(tracingListener);
            }

            // Prepare facts from context
            List<Object> facts = factPreparationService.prepareFacts(input.getContext());

            // Prepare execution context
            ValidationResult result = new ValidationResult();
            List<String> reasonCodes = new ArrayList<>();

            // Set globals
            session.setGlobal("result", result);
            session.setGlobal("reasonCodes", reasonCodes);

            // NOTE: Do NOT add result to facts - it should only be a global variable
            // If added to facts, the failure rule will check the fact (not the global),
            // causing it to always fire even when the main rule sets decision=ALLOW

            // Execute rules with timeout enforcement
            int timeoutMs = getTimeoutMs(input.getOptions());
            logger.info("BEFORE EXECUTION - executionId={}, facts={}, result.ok={}, result.decision={}, reasonCodes.size={}, timeoutMs={}",
                    executionId, facts.size(), result.getOk(), result.getDecision(), reasonCodes.size(), timeoutMs);
            logger.debug("Executing rules with {} facts for executionId: {}", facts.size(), executionId);

            executeWithTimeout(session, facts, timeoutMs, executionId);

            logger.info("AFTER EXECUTION - executionId={}, result.ok={}, result.decision={}, reasonCodes={}",
                    executionId, result.getOk(), result.getDecision(), reasonCodes);

            // Build response
            response = buildSuccessResponse(result, reasonCodes, startTime, tracingListener, input);

            // Record metrics
            boolean success = Boolean.TRUE.equals(response.getOk());
            metricsService.recordExecution(input.getBundleHash(),
                    Duration.ofMillis(response.getEngine().getLatencyMs()), success);

            if (sessionManager.isContainerCached(input.getBundleHash())) {
                metricsService.recordCacheHit(input.getBundleHash());
            } else {
                metricsService.recordCacheMiss(input.getBundleHash());
            }

            logger.info("Single rule execution completed: executionId={}, decision={}, latency={}ms",
                    executionId, response.getDecision(), response.getEngine().getLatencyMs());

        } catch (RuleExecutionTimeoutException e) {
            logger.error("Single rule execution timed out: executionId={}", executionId, e);
            response = buildTimeoutResponse(startTime);
        } catch (Exception e) {
            logger.error("Single rule execution failed: executionId={}, error={}", executionId, e.getMessage(), e);
            response = buildErrorResponse(startTime);
        }

        return response;
    }

    public List<ExecuteResponse> executeBatch(List<RuleEnginePort.ExecuteInput> inputs,
                                              Map<String, KieContainer> containersByBundle) {
        logger.info("Starting batch rule execution with {} inputs", inputs.size());
        long batchStartTime = System.currentTimeMillis();

        List<CompletableFuture<ExecuteResponse>> futures = new ArrayList<>();

        for (RuleEnginePort.ExecuteInput input : inputs) {
            KieContainer container = containersByBundle.get(input.getBundleHash());
            if (container == null) {
                futures.add(CompletableFuture.completedFuture(
                        buildBundleNotFoundResponse()
                ));
            } else {
                CompletableFuture<ExecuteResponse> future = CompletableFuture
                        .supplyAsync(() -> executeSingle(input, container), executionExecutor);
                futures.add(future);
            }
        }

        // Wait for all executions to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        try {
            allOf.join();
            List<ExecuteResponse> responses = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            // Record batch metrics
            long batchDuration = System.currentTimeMillis() - batchStartTime;
            int successCount = (int) responses.stream().filter(r -> r.getOk() != null && r.getOk()).count();
            metricsService.recordBatchExecution(inputs.size(), Duration.ofMillis(batchDuration), successCount);

            logger.info("Batch rule execution completed: {} responses", responses.size());
            return responses;
        } catch (Exception e) {
            logger.error("Batch rule execution failed", e);
            // Return error responses for any failed executions
            return futures.stream()
                    .map(future -> {
                        try {
                            return future.join();
                        } catch (Exception ex) {
                            return buildErrorResponse(System.currentTimeMillis());
                        }
                    })
                    .toList();
        }
    }

    public List<ExecuteResponse> executeOptimizedBatch(List<RuleEnginePort.ExecuteInput> inputs,
                                                       Map<String, KieContainer> containersByBundle) {
        logger.info("Starting optimized batch rule execution with {} inputs", inputs.size());

        // Group inputs by bundle hash for optimized execution
        Map<String, List<RuleEnginePort.ExecuteInput>> inputsByBundle = new HashMap<>();
        for (RuleEnginePort.ExecuteInput input : inputs) {
            inputsByBundle.computeIfAbsent(input.getBundleHash(), k -> new ArrayList<>()).add(input);
        }

        List<ExecuteResponse> allResponses = new ArrayList<>();

        for (Map.Entry<String, List<RuleEnginePort.ExecuteInput>> entry : inputsByBundle.entrySet()) {
            String bundleHash = entry.getKey();
            List<RuleEnginePort.ExecuteInput> bundleInputs = entry.getValue();
            KieContainer container = containersByBundle.get(bundleHash);

            if (container == null) {
                // Add error responses for missing bundle
                for (int i = 0; i < bundleInputs.size(); i++) {
                    allResponses.add(buildBundleNotFoundResponse());
                }
                continue;
            }

            try {
                List<ExecuteResponse> bundleResponses = executeBundleOptimized(bundleInputs, container);
                allResponses.addAll(bundleResponses);
            } catch (Exception e) {
                logger.error("Optimized batch execution failed for bundle: {}", bundleHash, e);
                // Add error responses for the failed bundle
                for (int i = 0; i < bundleInputs.size(); i++) {
                    allResponses.add(buildErrorResponse(System.currentTimeMillis()));
                }
            }
        }

        logger.info("Optimized batch rule execution completed: {} responses", allResponses.size());
        return allResponses;
    }

    private List<ExecuteResponse> executeBundleOptimized(List<RuleEnginePort.ExecuteInput> inputs,
                                                         KieContainer container) {
        String executionId = generateExecutionId();
        logger.debug("Executing optimized bundle batch: executionId={}, inputs={}", executionId, inputs.size());

        List<ExecuteResponse> responses = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Execute each input with its own session (thread-safe)
        // StatelessKieSession is NOT thread-safe, so we create a new session for each execution
        for (RuleEnginePort.ExecuteInput input : inputs) {
            try {
                ExecuteResponse response = executeSingleWithNewSession(input, container, executionId);
                responses.add(response);
            } catch (Exception e) {
                logger.error("Failed to execute single input in batch: executionId={}", executionId, e);
                responses.add(buildErrorResponse(startTime));
            }
        }

        return responses;
    }

    private ExecuteResponse executeSingleWithNewSession(RuleEnginePort.ExecuteInput input,
                                                        KieContainer container,
                                                        String executionId) {
        long startTime = System.currentTimeMillis();

        // Create a new session for this execution (thread-safe approach)
        StatelessKieSession session = sessionManager.createStatelessSession(container);

        // Set up tracing
        ExecutionTracingService.TracingAgendaEventListener tracingListener = null;
        if (shouldEnableExplain(input.getOptions())) {
            tracingListener = tracingService.createTracingListener(executionId);
            session.addEventListener(tracingListener);
        }

        // Prepare facts
        List<Object> facts = factPreparationService.prepareFacts(input.getContext());

        // Prepare execution context
        ValidationResult result = new ValidationResult();
        List<String> reasonCodes = new ArrayList<>();

        // Set globals
        session.setGlobal("result", result);
        session.setGlobal("reasonCodes", reasonCodes);

        // NOTE: Do NOT add result to facts - it should only be a global variable

        // Execute with timeout enforcement
        int timeoutMs = getTimeoutMs(input.getOptions());
        executeWithTimeout(session, facts, timeoutMs, executionId);

        return buildSuccessResponse(result, reasonCodes, startTime, tracingListener, input);
    }

    private ExecuteResponse buildSuccessResponse(ValidationResult result,
                                                 List<String> reasonCodes,
                                                 long startTime,
                                                 ExecutionTracingService.TracingAgendaEventListener tracingListener,
                                                 RuleEnginePort.ExecuteInput input) {
        logger.info("BUILD RESPONSE - result.ok={}, result.decision={}, reasonCodes={}, result.reasonCodes={}",
                result.getOk(), result.getDecision(), reasonCodes, result.getReasonCodes());

        ExecuteResponse response = new ExecuteResponse();

        response.setOk(Boolean.TRUE.equals(result.getOk()));
        response.setDecision(result.getDecision() != null ? result.getDecision() : "DENY");
        response.setReasonCodes(determineReasonCodes(reasonCodes, result));

        logger.info("RESPONSE BUILT - ok={}, decision={}, reasonCodes={}",
                response.getOk(), response.getDecision(), response.getReasonCodes());

        // Set explain entries
        if (tracingListener != null) {
            response.setExplain(tracingListener.getExplainEntries());
        } else {
            response.setExplain(List.of());
        }

        // Set engine info
        long latency = System.currentTimeMillis() - startTime;
        ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
        engine.setVersion("drools-10.1.0");
        engine.setLatencyMs((int) latency);
        engine.setCacheHit(sessionManager.isContainerCached(input.getBundleHash()));
        response.setEngine(engine);

        // Add per-rule timing metadata for performance analysis
        if (tracingListener != null) {
            Map<String, Object> metadata = new HashMap<>();
            Map<String, Long> ruleDurations = new HashMap<>();
            tracingListener.getRuleExecutionTimes().forEach((key, value) -> {
                if (key.endsWith("_duration")) {
                    ruleDurations.put(key.replace("_duration", ""), value);
                }
            });
            if (!ruleDurations.isEmpty()) {
                metadata.put("ruleTimings", ruleDurations);
                metadata.put("totalRulesFired", tracingListener.getTotalRulesFired());
                metadata.put("totalRuleExecutionMs", tracingListener.getTotalExecutionTime());
            }
            response.setMetadata(metadata);
        }

        return response;
    }

    private List<String> determineReasonCodes(List<String> reasonCodes, ValidationResult result) {
        if (!reasonCodes.isEmpty()) {
            return reasonCodes;
        }
        return result.getReasonCodes() != null ? result.getReasonCodes() : List.of();
    }

    private ExecuteResponse buildErrorResponse(long startTime) {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(false);
        response.setDecision("DENY");
        response.setReasonCodes(List.of("EXECUTION_ERROR"));
        response.setExplain(List.of(
                new ExecuteResponse.ExplainEntry(EXECUTION_ERROR, "exception", false)
        ));

        ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
        engine.setVersion(EXECUTION_ERROR);
        engine.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        engine.setCacheHit(false);
        response.setEngine(engine);

        return response;
    }

    private ExecuteResponse buildTimeoutResponse(long startTime) {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(false);
        response.setDecision("DENY");
        response.setReasonCodes(List.of("EXECUTION_TIMEOUT"));
        response.setExplain(List.of(
                new ExecuteResponse.ExplainEntry(EXECUTION_ERROR, "timeout", false)
        ));

        ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
        engine.setVersion("timeout");
        engine.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        engine.setCacheHit(false);
        response.setEngine(engine);

        return response;
    }

    private ExecuteResponse buildBundleNotFoundResponse() {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(false);
        response.setDecision("DENY");
        response.setReasonCodes(List.of("BUNDLE_NOT_FOUND"));
        response.setExplain(List.of(
                new ExecuteResponse.ExplainEntry("bundle", "not_found", false)
        ));

        ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
        engine.setVersion("N/A");
        engine.setLatencyMs(0);
        engine.setCacheHit(false);
        response.setEngine(engine);

        return response;
    }

    private boolean shouldEnableExplain(RuleEnginePort.ExecuteOptions options) {
        return options != null &&
                options.getExplain() != null &&
                !"NONE".equalsIgnoreCase(options.getExplain());
    }

    private String generateExecutionId() {
        return "exec_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get timeout from options, with fallback to default and minimum enforcement
     */
    private int getTimeoutMs(RuleEnginePort.ExecuteOptions options) {
        if (options == null || options.getTimeoutMs() == null || options.getTimeoutMs() <= 0) {
            return DEFAULT_TIMEOUT_MS;
        }
        return Math.max(options.getTimeoutMs(), MIN_TIMEOUT_MS);
    }

    /**
     * Execute session with timeout enforcement.
     * Wraps the blocking session.execute() call in a CompletableFuture with timeout.
     *
     * @param session the stateless KIE session
     * @param facts the facts to insert
     * @param timeoutMs timeout in milliseconds
     * @param executionId execution ID for logging
     * @throws RuleExecutionTimeoutException if execution exceeds timeout
     */
    private void executeWithTimeout(StatelessKieSession session, List<Object> facts,
                                     int timeoutMs, String executionId) {
        try {
            CompletableFuture<Void> executionFuture = CompletableFuture.runAsync(
                    () -> session.execute(facts),
                    executionExecutor
            );

            executionFuture.get(timeoutMs, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            logger.error("Rule execution timed out: executionId={}, timeoutMs={}", executionId, timeoutMs);
            throw new RuleExecutionTimeoutException(
                    String.format("Rule execution timed out after %dms: executionId=%s", timeoutMs, executionId));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleExecutionException("Rule execution was interrupted: executionId=" + executionId, e);
        } catch (Exception e) {
            throw new RuleExecutionException("Rule execution failed: executionId=" + executionId, e);
        }
    }

    /**
     * Exception thrown when rule execution exceeds the configured timeout
     */
    public static class RuleExecutionTimeoutException extends RuntimeException {
        public RuleExecutionTimeoutException(String message) {
            super(message);
        }
    }

    /**
     * General exception for rule execution failures
     */
    public static class RuleExecutionException extends RuntimeException {
        public RuleExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}