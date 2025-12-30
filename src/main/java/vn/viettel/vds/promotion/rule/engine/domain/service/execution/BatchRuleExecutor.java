package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class BatchRuleExecutor {

    private static final Logger logger = LoggerFactory.getLogger(BatchRuleExecutor.class);

    private final SingleRuleExecutor singleExecutor;
    private final ExecutionResponseBuilder responseBuilder;
    private final ExecutionMetricsService metricsService;
    private final Executor executionExecutor;

    public BatchRuleExecutor(SingleRuleExecutor singleExecutor,
                             ExecutionResponseBuilder responseBuilder,
                             ExecutionMetricsService metricsService) {
        this.singleExecutor = singleExecutor;
        this.responseBuilder = responseBuilder;
        this.metricsService = metricsService;
        this.executionExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public List<ExecuteResponse> execute(List<RuleEnginePort.ExecuteInput> inputs,
                                         Map<String, KieContainer> containersByBundle) {
        logger.info("Starting batch rule execution with {} inputs", inputs.size());
        long batchStartTime = System.currentTimeMillis();

        List<CompletableFuture<ExecuteResponse>> futures = createExecutionFutures(inputs, containersByBundle);
        
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            List<ExecuteResponse> responses = futures.stream().map(CompletableFuture::join).toList();
            
            recordBatchMetrics(inputs, batchStartTime, responses);
            return responses;
            
        } catch (Exception e) {
            logger.error("Batch rule execution failed", e);
            return createErrorResponses(futures);
        }
    }

    public List<ExecuteResponse> executeOptimized(List<RuleEnginePort.ExecuteInput> inputs,
                                                  Map<String, KieContainer> containersByBundle) {
        Map<String, List<RuleEnginePort.ExecuteInput>> inputsByBundle = groupInputsByBundle(inputs);
        List<ExecuteResponse> allResponses = new ArrayList<>();

        for (Map.Entry<String, List<RuleEnginePort.ExecuteInput>> entry : inputsByBundle.entrySet()) {
            String bundleHash = entry.getKey();
            List<RuleEnginePort.ExecuteInput> bundleInputs = entry.getValue();
            KieContainer container = containersByBundle.get(bundleHash);

            if (container == null) {
                addBundleNotFoundResponses(allResponses, bundleInputs.size());
                continue;
            }

            try {
                List<ExecuteResponse> bundleResponses = executeBundleInputs(bundleInputs, container);
                allResponses.addAll(bundleResponses);
            } catch (Exception e) {
                logger.error("Optimized batch execution failed for bundle: {}", bundleHash, e);
                addErrorResponses(allResponses, bundleInputs.size());
            }
        }

        return allResponses;
    }

    private List<CompletableFuture<ExecuteResponse>> createExecutionFutures(
            List<RuleEnginePort.ExecuteInput> inputs,
            Map<String, KieContainer> containersByBundle) {
        
        List<CompletableFuture<ExecuteResponse>> futures = new ArrayList<>();
        
        for (RuleEnginePort.ExecuteInput input : inputs) {
            KieContainer container = containersByBundle.get(input.getBundleHash());
            if (container == null) {
                futures.add(CompletableFuture.completedFuture(responseBuilder.buildBundleNotFoundResponse()));
            } else {
                CompletableFuture<ExecuteResponse> future = CompletableFuture
                        .supplyAsync(() -> singleExecutor.execute(input, container), executionExecutor);
                futures.add(future);
            }
        }
        
        return futures;
    }

    private Map<String, List<RuleEnginePort.ExecuteInput>> groupInputsByBundle(List<RuleEnginePort.ExecuteInput> inputs) {
        Map<String, List<RuleEnginePort.ExecuteInput>> inputsByBundle = new HashMap<>();
        for (RuleEnginePort.ExecuteInput input : inputs) {
            inputsByBundle.computeIfAbsent(input.getBundleHash(), k -> new ArrayList<>()).add(input);
        }
        return inputsByBundle;
    }

    private List<ExecuteResponse> executeBundleInputs(List<RuleEnginePort.ExecuteInput> inputs, KieContainer container) {
        List<ExecuteResponse> responses = new ArrayList<>();
        for (RuleEnginePort.ExecuteInput input : inputs) {
            responses.add(singleExecutor.execute(input, container));
        }
        return responses;
    }

    private void recordBatchMetrics(List<RuleEnginePort.ExecuteInput> inputs, long batchStartTime, List<ExecuteResponse> responses) {
        long batchDuration = System.currentTimeMillis() - batchStartTime;
        int successCount = (int) responses.stream().filter(r -> r.getOk() != null && r.getOk()).count();
        metricsService.recordBatchExecution(inputs.size(), Duration.ofMillis(batchDuration), successCount);
    }

    private List<ExecuteResponse> createErrorResponses(List<CompletableFuture<ExecuteResponse>> futures) {
        return futures.stream()
                .map(future -> {
                    try {
                        return future.join();
                    } catch (Exception ex) {
                        return responseBuilder.buildErrorResponse(System.currentTimeMillis());
                    }
                })
                .toList();
    }

    private void addBundleNotFoundResponses(List<ExecuteResponse> responses, int count) {
        for (int i = 0; i < count; i++) {
            responses.add(responseBuilder.buildBundleNotFoundResponse());
        }
    }

    private void addErrorResponses(List<ExecuteResponse> responses, int count) {
        for (int i = 0; i < count; i++) {
            responses.add(responseBuilder.buildErrorResponse(System.currentTimeMillis()));
        }
    }
}
