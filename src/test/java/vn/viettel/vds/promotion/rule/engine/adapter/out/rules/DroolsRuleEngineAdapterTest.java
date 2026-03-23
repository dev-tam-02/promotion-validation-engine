package vn.viettel.vds.promotion.rule.engine.adapter.out.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ObjectStoragePort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.rule.engine.domain.service.DroolsCompilationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.RuleTranslationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.TemporalDrlGenerator;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.ExecutionMetricsService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.KieSessionManager;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.RuleExecutionOrchestrator;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslatorRegistry;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests DroolsRuleEngineAdapter with mocked storage/repository adapters
 * but REAL Drools compilation and translation services.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DroolsRuleEngineAdapter — compile & execute with mocked adapters")
class DroolsRuleEngineAdapterTest {

    @Mock private KieSessionManager sessionManager;
    @Mock private RuleExecutionOrchestrator executionOrchestrator;
    @Mock private ExecutionMetricsService metricsService;
    @Mock private BundleRepositoryPort bundleRepositoryPort;
    @Mock private ObjectStoragePort objectStoragePort;
    @Mock private TemporalDrlGenerator temporalDrlGenerator;

    // Real services (no mocks — we test actual DRL generation + Drools compilation)
    private RuleTranslationService translationService;
    private DroolsCompilationService compilationService;
    private DroolsRuleEngineAdapter adapter;

    @BeforeEach
    void setUp() {
        List<vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator> translators = List.of(
                new OrderTotalGteOperatorTranslator(),
                new CustomerSegmentInOperatorTranslator(),
                new OrderItemCategoryInOperatorTranslator(),
                new OrderItemBrandInOperatorTranslator(),
                new BudgetRedemptionsTotalLteOperatorTranslator()
        );
        OperatorTranslatorRegistry registry = new OperatorTranslatorRegistry(translators);
        translationService = new RuleTranslationService(registry);
        compilationService = new DroolsCompilationService();

        adapter = new DroolsRuleEngineAdapter(
                translationService, compilationService, sessionManager,
                executionOrchestrator, metricsService,
                bundleRepositoryPort, objectStoragePort, temporalDrlGenerator);
    }

    @Nested
    @DisplayName("compile()")
    class Compile {

        @Test
        @DisplayName("compiles simple rule and returns bundleHash + artifact")
        void compilesSimpleRule() {
            RuleEnginePort.CompileInput input = new RuleEnginePort.CompileInput(
                    "rule-001", 1,
                    List.of(
                            Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("c1")),
                            Map.<String, Object>of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                                    "operatorVersion", 1, "params", Map.of("amount", "50000"), "reasonCode", "MIN_ORDER")
                    ),
                    null, null
            );

            RuleEnginePort.CompileResult result = adapter.compile(input);

            assertNotNull(result);
            assertNotNull(result.getBundleHash(), "bundleHash must not be null");
            assertTrue(result.getBundleHash().startsWith("sha256:"), "bundleHash must start with sha256:");
            assertNotNull(result.getArtifactBytes(), "artifact bytes must not be null");
            assertTrue(result.getArtifactBytes().length > 0, "artifact must have content");
            assertNotNull(result.getDrlContent(), "DRL content must be returned");
            assertFalse(result.getDrlContent().contains("System.out.println"),
                    "Generated DRL must not contain System.out.println");
        }

        @Test
        @DisplayName("produces deterministic bundleHash for same input")
        void deterministicBundleHash() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("c1")),
                    Map.<String, Object>of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100"), "reasonCode", "R1")
            );

            RuleEnginePort.CompileInput input1 = new RuleEnginePort.CompileInput("rule-1", 1, nodes, null, null);
            RuleEnginePort.CompileInput input2 = new RuleEnginePort.CompileInput("rule-1", 1, nodes, null, null);

            RuleEnginePort.CompileResult r1 = adapter.compile(input1);
            RuleEnginePort.CompileResult r2 = adapter.compile(input2);

            assertEquals(r1.getBundleHash(), r2.getBundleHash(),
                    "Same input must produce same bundleHash");
        }

        @Test
        @DisplayName("compiles multi-condition ALL rule")
        void compilesMultiConditionRule() {
            RuleEnginePort.CompileInput input = new RuleEnginePort.CompileInput(
                    "rule-complex", 1,
                    List.of(
                            Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL",
                                    "children", List.of("c1", "c2", "c3")),
                            Map.<String, Object>of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                                    "operatorVersion", 1, "params", Map.of("amount", "50000"), "reasonCode", "R1"),
                            Map.<String, Object>of("id", "c2", "type", "COND", "operatorName", "customer.in_segment",
                                    "operatorVersion", 1, "params", Map.of("segments", List.of("VIP")), "reasonCode", "R2"),
                            Map.<String, Object>of("id", "c3", "type", "COND", "operatorName", "order.item.brand.in",
                                    "operatorVersion", 1, "params", Map.of("brands", List.of("Nike")), "reasonCode", "R3")
                    ),
                    null, null
            );

            RuleEnginePort.CompileResult result = adapter.compile(input);

            assertNotNull(result.getBundleHash());
            assertTrue(result.getDrlContent().contains("Order(total != null"));
            assertTrue(result.getDrlContent().contains("Customer(segments"));
            assertTrue(result.getDrlContent().contains("from $order.getItems()"));
        }
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        @DisplayName("delegates to orchestrator with correct bundleHash")
        void delegatesToOrchestrator() {
            // Mock container lookup
            org.kie.api.runtime.KieContainer mockContainer = mock(org.kie.api.runtime.KieContainer.class);
            when(sessionManager.getCachedContainer("test-hash")).thenReturn(mockContainer);

            ExecuteResponse mockResponse = new ExecuteResponse();
            mockResponse.setOk(true);
            mockResponse.setDecision("ALLOW");
            when(executionOrchestrator.executeSingle(any(), eq(mockContainer))).thenReturn(mockResponse);

            RuleEnginePort.ExecuteInput input = new RuleEnginePort.ExecuteInput(
                    "test-hash",
                    Map.of("customer", Map.of("id", "c1"), "order", Map.of("id", "o1", "total", 100000)),
                    null
            );

            ExecuteResponse response = adapter.execute(input);

            assertNotNull(response);
            assertEquals("ALLOW", response.getDecision());
            verify(executionOrchestrator).executeSingle(any(), eq(mockContainer));
        }

        @Test
        @DisplayName("returns DENY when container not found and no fallback")
        void denyWhenContainerNotFound() {
            when(sessionManager.getCachedContainer("missing-hash")).thenReturn(null);
            // Mock: no artifact in cache, no bundle in DB
            when(bundleRepositoryPort.findById("missing-hash")).thenReturn(java.util.Optional.empty());

            RuleEnginePort.ExecuteInput input = new RuleEnginePort.ExecuteInput(
                    "missing-hash",
                    Map.of("customer", Map.of("id", "c1")),
                    null
            );

            ExecuteResponse response = adapter.execute(input);

            assertNotNull(response);
            // Should return error/DENY response
            assertFalse(Boolean.TRUE.equals(response.getOk()));
        }
    }
}
