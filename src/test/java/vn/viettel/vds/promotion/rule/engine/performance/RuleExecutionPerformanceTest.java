package vn.viettel.vds.promotion.rule.engine.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.BatchRuleExecutor;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.SingleRuleExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleExecutionPerformanceTest {

    @Mock
    private SingleRuleExecutor singleExecutor;

    @Test
    void batchExecution_ShouldCompleteWithinTimeLimit() {
        // Given
        int batchSize = 1000;
        List<RuleEnginePort.ExecuteInput> inputs = createTestInputs(batchSize);
        
        ExecuteResponse mockResponse = new ExecuteResponse();
        mockResponse.setOk(true);
        mockResponse.setDecision("ALLOW");
        
        when(singleExecutor.execute(any(), any())).thenReturn(mockResponse);
        
        BatchRuleExecutor batchExecutor = new BatchRuleExecutor(singleExecutor, null, null);
        
        // When
        long startTime = System.nanoTime();
        List<ExecuteResponse> results = batchExecutor.executeOptimized(inputs, Map.of());
        long endTime = System.nanoTime();
        
        // Then
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        assertTrue(durationMs < 5000, "Batch execution should complete within 5 seconds");
        // Note: This is a mock test - real performance testing would require actual rule execution
    }

    private List<RuleEnginePort.ExecuteInput> createTestInputs(int count) {
        List<RuleEnginePort.ExecuteInput> inputs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> context = new HashMap<>();
            context.put("customer", Map.of("id", "customer-" + i));
            inputs.add(new RuleEnginePort.ExecuteInput("tenant1", "bundle1", context, null));
        }
        return inputs;
    }
}
