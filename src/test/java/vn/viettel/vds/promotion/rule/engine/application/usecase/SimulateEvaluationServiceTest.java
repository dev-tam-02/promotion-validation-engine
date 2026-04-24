package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.mockito.Mockito;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.EvaluateRuleResponse;
import vn.viettel.vds.promotion.rule.engine.adapter.out.registry.InMemoryRuleRegistryAdapter;
import vn.viettel.vds.promotion.rule.engine.application.service.QuotaCounterService;
import vn.viettel.vds.promotion.rule.engine.application.usecase.SimulateEvaluationService.SimulateAgendaEventListener;
import vn.viettel.vds.promotion.rule.engine.domain.model.Customer;
import vn.viettel.vds.promotion.rule.engine.domain.model.CustomerFact;
import vn.viettel.vds.promotion.rule.engine.domain.model.Order;
import vn.viettel.vds.promotion.rule.engine.domain.model.RegisteredRule;
import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;
import vn.viettel.vds.promotion.rule.engine.domain.service.DroolsCompilationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.FactPreparationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.KieSessionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link SimulateEvaluationService}.
 *
 * <p>Uses real Drools engine (no mocks for KieSession) to verify that
 * the SIMULATE mode correctly traces per-declaration matches.
 *
 * <p>Test scenario: spec §4.4.4 example — order.total.gte 500K + customer.in_segment VIP
 * combined in one DRL rule with two bound declarations.
 */
@DisplayName("SimulateEvaluationService — SIMULATE mode trace")
class SimulateEvaluationServiceTest {

    // -----------------------------------------------------------------------
    // DRL used in these tests:
    //   - binds $order: Order (total >= 500000, currency == "VND")
    //   - binds $customer: CustomerFact (segments contains "seg_vip")
    //   - fires → sets result.decision = "ALLOW"
    // Two bound declarations → two matchedNodes after afterMatchFired
    // -----------------------------------------------------------------------
    private static final String SPEC_DRL = """
            package rules;

            import vn.viettel.vds.promotion.rule.engine.domain.model.Order;
            import vn.viettel.vds.promotion.rule.engine.domain.model.CustomerFact;
            import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;
            import vn.viettel.vds.promotion.rule.engine.domain.model.RuleMatched;
            import java.math.BigDecimal;
            import java.util.List;

            global ValidationResult result;
            global List reasonCodes;

            rule "spec-4-4-4-example"
            when
                $order    : Order(total >= 500000B, currency == "VND")
                $customer : CustomerFact(segments contains "seg_vip")
            then
                result.setDecision("ALLOW");
                result.setOk(true);
                insert(new RuleMatched());
            end

            rule "spec-4-4-4-example-deny"
            salience -1
            when
                not RuleMatched()
            then
                result.setDecision("DENY");
                result.setOk(false);
            end
            """;

    private static final String RULE_ID = "test-simulate-rule";

    private KieContainer specContainer;
    private InMemoryRuleRegistryAdapter registry;

    @BeforeEach
    void setUp() {
        specContainer = compileToContainer(SPEC_DRL);
        registry = new InMemoryRuleRegistryAdapter();
        registry.save(new RegisteredRule(RULE_ID, SPEC_DRL, "hash-simulate-test",
                Instant.now(), Instant.now()));
    }

    // -----------------------------------------------------------------------
    // Direct listener tests (no service layer)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("SimulateAgendaEventListener — direct Drools session")
    class ListenerDirectTests {

        @Test
        @DisplayName("ALLOW facts → afterMatchFired captures 2 declarations ($order, $customer)")
        void allowFacts_twoBoundDeclarations() {
            StatelessKieSession session = specContainer.newStatelessKieSession();

            SimulateAgendaEventListener listener = new SimulateAgendaEventListener();
            session.addEventListener(listener);

            ValidationResult result = new ValidationResult();
            session.setGlobal("result", result);
            session.setGlobal("reasonCodes", new ArrayList<>());

            Order order = new Order("o1");
            order.setTotal(BigDecimal.valueOf(600_000));
            order.setCurrency("VND");

            CustomerFact customerFact = new CustomerFact("c1", Set.of("seg_vip"), null);

            session.execute(List.of(order, customerFact, result));

            assertThat(result.getDecision()).isEqualTo("ALLOW");

            // Both $order and $customer should be in matchedNodes
            assertThat(listener.getMatchedNodes())
                    .as("Two declarations should be matched: $order and $customer")
                    .hasSize(2)
                    .containsExactlyInAnyOrder("$order", "$customer");

            assertThat(listener.buildTrace())
                    .as("Trace should have one entry per matched declaration")
                    .hasSize(2);

            assertThat(listener.getUnmatchedNodes()).isEmpty();
        }

        @Test
        @DisplayName("DENY facts (order total too low) → no match for main rule, deny rule fires")
        void denyFacts_orderTotalTooLow() {
            StatelessKieSession session = specContainer.newStatelessKieSession();

            SimulateAgendaEventListener listener = new SimulateAgendaEventListener();
            session.addEventListener(listener);

            ValidationResult result = new ValidationResult();
            session.setGlobal("result", result);
            session.setGlobal("reasonCodes", new ArrayList<>());

            Order order = new Order("o1");
            order.setTotal(BigDecimal.valueOf(100_000)); // below threshold
            order.setCurrency("VND");

            CustomerFact customerFact = new CustomerFact("c1", Set.of("seg_vip"), null);

            session.execute(List.of(order, customerFact, result));

            // Main rule should NOT fire (order total too low) → DENY
            // matchedNodes should NOT include $order or $customer from the main rule
            assertThat(listener.getMatchedNodes())
                    .as("Main rule did not fire, so $order and $customer should NOT be matched")
                    .doesNotContain("$order", "$customer");
        }
    }

    // -----------------------------------------------------------------------
    // SimulateEvaluationService integration tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("SimulateEvaluationService.evaluate() — integration")
    class ServiceIntegrationTests {

        private SimulateEvaluationService service;

        @BeforeEach
        void setUpService() {
            // Use a real DroolsCompilationService (no-arg, calls KieServices.Factory.get() internally)
            DroolsCompilationService compilationService = new DroolsCompilationService();

            // Build a minimal KieSessionManager (pool disabled)
            vn.viettel.vds.promotion.rule.engine.domain.service.session.SessionPoolConfig poolConfig =
                    new vn.viettel.vds.promotion.rule.engine.domain.service.session.SessionPoolConfig();
            poolConfig.setEnablePooling(false);
            poolConfig.setMaxPoolSize(1);

            vn.viettel.vds.promotion.rule.engine.domain.service.session.KieSessionFactory sessionFactory =
                    new vn.viettel.vds.promotion.rule.engine.domain.service.session.KieSessionFactory();

            vn.viettel.vds.promotion.rule.engine.domain.service.session.KieSessionPool sessionPool =
                    new vn.viettel.vds.promotion.rule.engine.domain.service.session.KieSessionPool(sessionFactory, poolConfig);

            KieSessionManager sessionManager = new KieSessionManager(sessionPool, poolConfig, sessionFactory);
            sessionManager.initializeSessionFactory();

            // Pre-cache the container for the registered rule bundle hash
            sessionManager.cacheContainer("hash-simulate-test", specContainer);

            FactPreparationService factPreparationService = new FactPreparationService();

            service = new SimulateEvaluationService(
                    registry,
                    compilationService,
                    factPreparationService,
                    sessionManager,
                    Mockito.mock(QuotaCounterService.class)
            );
        }

        @Test
        @DisplayName("ALLOW facts via service → verdict=ALLOW, trace has 2 matched nodes")
        void allowFacts_viaService_verdictAllowTwoNodes() {
            // Pass domain objects directly (FactPreparationService handles Customer/Order/CustomerFact)
            Customer customer = new Customer("c1", Set.of("seg_vip"));
            CustomerFact customerFact = new CustomerFact("c1", Set.of("seg_vip"), null);
            Order order = new Order("o1");
            order.setTotal(BigDecimal.valueOf(600_000));
            order.setCurrency("VND");

            // facts map: customer key → FactPreparationService creates Customer+CustomerFact
            // But CustomerFact is already added from Customer — for this test, use CustomerFact directly
            // by providing it under "customer" key (prepareFacts checks Customer first)
            Map<String, Object> facts = Map.of(
                    "customer", customer,
                    "order", order
            );

            EvaluateRuleResponse response = service.evaluate(
                    List.of(RULE_ID),
                    facts,
                    true  // simulateMode
            );

            assertThat(response.getVerdict()).isEqualTo("ALLOW");
            assertThat(response.getMatchedNodes())
                    .as("Two bound declarations should appear as matched nodes")
                    .hasSize(2);
            assertThat(response.getTrace())
                    .as("Trace should have one entry per matched node")
                    .hasSize(2);
            assertThat(response.getUnmatchedNodes()).isEmpty();
        }

        @Test
        @DisplayName("Unknown ruleId → verdict=DENY, reason RULE_NOT_REGISTERED")
        void unknownRuleId_verdictDeny() {
            EvaluateRuleResponse response = service.evaluate(
                    List.of("non-existent-rule"),
                    Map.of("order", new Order("o1")),
                    true
            );

            assertThat(response.getVerdict()).isEqualTo("DENY");
            assertThat(response.getReasonCodes()).contains("RULE_NOT_REGISTERED");
        }

        @Test
        @DisplayName("Normal mode (non-simulate) → verdict returned, trace empty")
        void normalMode_traceEmpty() {
            Customer customer = new Customer("c1", Set.of("seg_vip"));
            Order order = new Order("o1");
            order.setTotal(BigDecimal.valueOf(600_000));
            order.setCurrency("VND");

            EvaluateRuleResponse response = service.evaluate(
                    List.of(RULE_ID),
                    Map.of("customer", customer, "order", order),
                    false  // not simulate
            );

            assertThat(response.getVerdict()).isEqualTo("ALLOW");
            assertThat(response.getTrace()).isEmpty();
            assertThat(response.getMatchedNodes()).isEmpty();
            assertThat(response.getUnmatchedNodes()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private KieContainer compileToContainer(String drl) {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/rules/simulate-test.drl", drl);
        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();

        if (kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            fail("DRL compile error:\n" + kb.getResults().getMessages());
        }

        KieModule module = kb.getKieModule();
        return ks.newKieContainer(module.getReleaseId());
    }
}
