package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchRuleExecutorTest {

    @Mock
    private SingleRuleExecutor singleExecutor;
    @Mock
    private ExecutionResponseBuilder responseBuilder;
    @Mock
    private ExecutionMetricsService metricsService;
    @Mock
    private KieContainer container;

    private BatchRuleExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new BatchRuleExecutor(singleExecutor, responseBuilder, metricsService);
    }

    @Test
    void execute_ShouldReturnResponses_ForAllInputs() {
        // Given
        List<RuleEnginePort.ExecuteInput> inputs = List.of(
                createTestInput("bundle1"),
                createTestInput("bundle2")
        );
        Map<String, KieContainer> containers = Map.of(
                "bundle1", container,
                "bundle2", container
        );

        ExecuteResponse successResponse = createSuccessResponse();
        when(singleExecutor.execute(any(), any())).thenReturn(successResponse);

        // When
        List<ExecuteResponse> results = executor.execute(inputs, containers);

        // Then
        assertEquals(2, results.size());
        results.forEach(response -> {
            assertTrue(response.getOk());
            assertEquals("ALLOW", response.getDecision());
        });
        verify(singleExecutor, times(2)).execute(any(), any());
    }

    @Test
    void execute_ShouldReturnBundleNotFoundResponse_WhenContainerMissing() {
        // Given
        List<RuleEnginePort.ExecuteInput> inputs = List.of(createTestInput("missing-bundle"));
        Map<String, KieContainer> containers = Map.of();

        ExecuteResponse notFoundResponse = createBundleNotFoundResponse();
        when(responseBuilder.buildBundleNotFoundResponse()).thenReturn(notFoundResponse);

        // When
        List<ExecuteResponse> results = executor.execute(inputs, containers);

        // Then
        assertEquals(1, results.size());
        assertFalse(results.get(0).getOk());
        assertEquals("DENY", results.get(0).getDecision());
        verify(responseBuilder).buildBundleNotFoundResponse();
    }

    private RuleEnginePort.ExecuteInput createTestInput(String bundleHash) {
        Map<String, Object> context = new HashMap<>();
        return new RuleEnginePort.ExecuteInput(bundleHash, context, null);
    }

    private ExecuteResponse createSuccessResponse() {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(true);
        response.setDecision("ALLOW");
        return response;
    }

    private ExecuteResponse createBundleNotFoundResponse() {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(false);
        response.setDecision("DENY");
        response.setReasonCodes(List.of("BUNDLE_NOT_FOUND"));
        return response;
    }
}
