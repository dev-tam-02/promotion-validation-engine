package vn.viettel.vds.promotion.validation.engine.domain.service.execution;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;
import vn.viettel.vds.promotion.validation.engine.domain.service.session.SessionPoolConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class RuleExecutionOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(RuleExecutionOrchestrator.class);

    private final KieSessionManager sessionManager;
    private final FactPreparationService factPreparationService;
    private final ExecutionTracingService tracingService;
    private final ExecutionMetricsService metricsService;
    private final Executor executionExecutor;
    private final SessionPoolConfig poolConfig;

    public RuleExecutionOrchestrator(KieSessionManager sessionManager,
                                     FactPreparationService factPreparationService,
                                     ExecutionTracingService tracingService,
                                     ExecutionMetricsService metricsService,
                                     SessionPoolConfig poolConfig) {
        this.sessionManager = sessionManager;
        this.factPreparationService = factPreparationService;
        this.tracingService = tracingService;
        this.metricsService = metricsService;
        this.poolConfig = poolConfig;
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

            // Add result to facts
            facts.add(result);

            // Execute rules
            logger.debug("Executing rules with {} facts for executionId: {}", facts.size(), executionId);
            session.execute(facts);

            // Build response
            response = buildSuccessResponse(result, reasonCodes, startTime, tracingListener, input);

            // Record metrics
            boolean success = response.getOk() != null ? response.getOk() : false;
            metricsService.recordExecution(input.getBundleHash(),
                    Duration.ofMillis(response.getEngine().getLatencyMs()), success);

            if (sessionManager.isContainerCached(input.getBundleHash())) {
                metricsService.recordCacheHit(input.getBundleHash());
            } else {
                metricsService.recordCacheMiss(input.getBundleHash());
            }

            logger.info("Single rule execution completed: executionId={}, decision={}, latency={}ms",
                    executionId, response.getDecision(), response.getEngine().getLatencyMs());

        } catch (Exception e) {
            logger.error("Single rule execution failed: executionId={}, error={}", executionId, e.getMessage(), e);
            response = buildErrorResponse(startTime, e);
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
                        buildBundleNotFoundResponse(input.getBundleHash())
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
            int successCount = (int) responses.stream().mapToLong(r -> r.getOk() != null && r.getOk() ? 1 : 0).sum();
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
                            return buildErrorResponse(System.currentTimeMillis(), ex);
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
                for (RuleEnginePort.ExecuteInput input : bundleInputs) {
                    allResponses.add(buildBundleNotFoundResponse(bundleHash));
                }
                continue;
            }

            try {
                List<ExecuteResponse> bundleResponses = executeBundleOptimized(bundleInputs, container);
                allResponses.addAll(bundleResponses);
            } catch (Exception e) {
                logger.error("Optimized batch execution failed for bundle: {}", bundleHash, e);
                // Add error responses for the failed bundle
                for (RuleEnginePort.ExecuteInput input : bundleInputs) {
                    allResponses.add(buildErrorResponse(System.currentTimeMillis(), e));
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

        try {
            StatelessKieSession session = sessionManager.createStatelessSession(container);

            // Process each input individually but reuse session setup
            for (RuleEnginePort.ExecuteInput input : inputs) {
                try {
                    ExecuteResponse response = executeSingleInSession(input, session, executionId);
                    responses.add(response);
                } catch (Exception e) {
                    logger.error("Failed to execute single input in batch: executionId={}", executionId, e);
                    responses.add(buildErrorResponse(startTime, e));
                }
            }

        } catch (Exception e) {
            logger.error("Failed to create session for bundle batch execution: executionId={}", executionId, e);
            // Return error responses for all inputs
            for (RuleEnginePort.ExecuteInput input : inputs) {
                responses.add(buildErrorResponse(startTime, e));
            }
        }

        return responses;
    }

    private ExecuteResponse executeSingleInSession(RuleEnginePort.ExecuteInput input,
                                                   StatelessKieSession session,
                                                   String executionId) {
        long startTime = System.currentTimeMillis();

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

        facts.add(result);

        // Execute
        session.execute(facts);

        // Remove listener after execution
        if (tracingListener != null) {
            session.removeEventListener(tracingListener);
        }

        return buildSuccessResponse(result, reasonCodes, startTime, tracingListener, input);
    }

    private ExecuteResponse buildSuccessResponse(ValidationResult result,
                                                 List<String> reasonCodes,
                                                 long startTime,
                                                 ExecutionTracingService.TracingAgendaEventListener tracingListener,
                                                 RuleEnginePort.ExecuteInput input) {
        ExecuteResponse response = new ExecuteResponse();

        response.setOk(result.getOk() != null ? result.getOk() : false);
        response.setDecision(result.getDecision() != null ? result.getDecision() : "DENY");
        response.setReasonCodes(reasonCodes.isEmpty() ?
                (result.getReasonCodes() != null ? result.getReasonCodes() : List.of()) : reasonCodes);

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

        return response;
    }

    private ExecuteResponse buildErrorResponse(long startTime, Exception error) {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(false);
        response.setDecision("DENY");
        response.setReasonCodes(List.of("EXECUTION_ERROR"));
        response.setExplain(List.of(
                new ExecuteResponse.ExplainEntry("error", "exception", false)
        ));

        ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
        engine.setVersion("error");
        engine.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        engine.setCacheHit(false);
        response.setEngine(engine);

        return response;
    }

    private ExecuteResponse buildBundleNotFoundResponse(String bundleHash) {
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
}