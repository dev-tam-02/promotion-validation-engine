package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SingleRuleExecutorTest {

    @Mock
    private KieSessionManager sessionManager;
    @Mock
    private FactPreparationService factPreparationService;
    @Mock
    private ExecutionTracingService tracingService;
    @Mock
    private ExecutionResponseBuilder responseBuilder;
    @Mock
    private ExecutionErrorHandler errorHandler;
    @Mock
    private KieContainer container;
    @Mock
    private StatelessKieSession session;

    private SingleRuleExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new SingleRuleExecutor(sessionManager, factPreparationService, tracingService, responseBuilder, errorHandler);
    }

    @Test
    void execute_ShouldReturnSuccessResponse_WhenRuleExecutionSucceeds() {
        // Given
        RuleEnginePort.ExecuteInput input = createTestInput();
        ExecuteResponse expectedResponse = createSuccessResponse();

        when(sessionManager.createStatelessSession(container)).thenReturn(session);
        when(factPreparationService.prepareFacts(any())).thenReturn(List.of());
        when(responseBuilder.buildSuccessResponse(any(), any(), anyLong(), any(), any()))
                .thenReturn(expectedResponse);

        // When
        ExecuteResponse result = executor.execute(input, container);

        // Then
        assertNotNull(result);
        assertTrue(result.getOk());
        assertEquals("ALLOW", result.getDecision());
        verify(session).execute(any(Iterable.class));
    }

    @Test
    void execute_ShouldReturnErrorResponse_WhenExceptionOccurs() {
        // Given
        RuleEnginePort.ExecuteInput input = createTestInput();
        ExecuteResponse errorResponse = createErrorResponse();

        when(sessionManager.createStatelessSession(container)).thenThrow(new RuntimeException("Session error"));
        when(responseBuilder.buildErrorResponse(anyLong())).thenReturn(errorResponse);

        // When
        ExecuteResponse result = executor.execute(input, container);

        // Then
        assertNotNull(result);
        assertFalse(result.getOk());
        assertEquals("DENY", result.getDecision());
        verify(responseBuilder).buildErrorResponse(anyLong());
    }

    private RuleEnginePort.ExecuteInput createTestInput() {
        Map<String, Object> context = new HashMap<>();
        return new RuleEnginePort.ExecuteInput("tenant1", "bundle123", context, null);
    }

    private ExecuteResponse createSuccessResponse() {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(true);
        response.setDecision("ALLOW");
        return response;
    }

    private ExecuteResponse createErrorResponse() {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(false);
        response.setDecision("DENY");
        return response;
    }
}
