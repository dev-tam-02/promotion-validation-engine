package vn.viettel.vds.promotion.rule.engine.adapter.out.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.EvaluateRuleResponse;
import vn.viettel.vds.promotion.rule.engine.adapter.out.registry.InMemoryRuleRegistryAdapter;
import vn.viettel.vds.promotion.rule.engine.application.service.QuotaCounterService;
import vn.viettel.vds.promotion.rule.engine.application.usecase.SimulateEvaluationService;
import vn.viettel.vds.promotion.rule.engine.domain.model.Customer;
import vn.viettel.vds.promotion.rule.engine.domain.model.CustomerFact;
import vn.viettel.vds.promotion.rule.engine.domain.model.Order;
import vn.viettel.vds.promotion.rule.engine.domain.model.RegisteredRule;
import vn.viettel.vds.promotion.rule.engine.domain.service.DroolsCompilationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.FactPreparationService;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.KieSessionManager;
import vn.viettel.vds.promotion.rule.engine.domain.service.session.KieSessionFactory;
import vn.viettel.vds.promotion.rule.engine.domain.service.session.KieSessionPool;
import vn.viettel.vds.promotion.rule.engine.domain.service.session.SessionPoolConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link RuleAnalyticsMetrics} instrumentation (Task 09 V10).
 *
 * <p>Verifies:
 * <ul>
 *   <li>ALLOW evaluation → {@code rule_fires_total{verdict=ALLOW}} incremented.</li>
 *   <li>DENY evaluation → {@code rule_fires_total{verdict=DENY}} + {@code rule_rejects_total} incremented.</li>
 *   <li>Histogram observation recorded in {@code rule_evaluation_duration_seconds}.</li>
 *   <li>All three metric names are exposed (presence check).</li>
 * </ul>
 */
@DisplayName("MetricsRecordingTest — rule analytics metrics (Task 09 V10)")
class MetricsRecordingTest {

    // -----------------------------------------------------------------------
    // DRL: ALLOW rule (order.total >= 500_000 VND + seg_vip)
    // -----------------------------------------------------------------------
    private static final String ALLOW_DRL = """
            package rules;

            import vn.viettel.vds.promotion.rule.engine.domain.model.Order;
            import vn.viettel.vds.promotion.rule.engine.domain.model.CustomerFact;
            import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;
            import vn.viettel.vds.promotion.rule.engine.domain.model.RuleMatched;
            import java.math.BigDecimal;
            import java.util.List;

            global ValidationResult result;
            global List reasonCodes;

            rule "allow-vip-order"
            when
                $order    : Order(total >= 500000B, currency == "VND")
                $customer : CustomerFact(segments contains "seg_vip")
            then
                result.setDecision("ALLOW");
                result.setOk(true);
                insert(new RuleMatched());
            end

            rule "deny-fallback"
            salience -1
            when
                not RuleMatched()
            then
                result.setDecision("DENY");
                result.setOk(false);
                reasonCodes.add("CONDITION_NOT_MET");
            end
            """;

    private static final String RULE_ID = "metrics-test-rule-001";

    private MeterRegistry meterRegistry;
    private RuleAnalyticsMetrics analyticsMetrics;
    private SimulateEvaluationService service;
    private KieContainer allowContainer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        analyticsMetrics = new RuleAnalyticsMetrics(meterRegistry);

        allowContainer = compileToContainer(ALLOW_DRL);

        InMemoryRuleRegistryAdapter registry = new InMemoryRuleRegistryAdapter();
        registry.save(new RegisteredRule(RULE_ID, ALLOW_DRL, "hash-metrics-test",
                Instant.now(), Instant.now()));

        DroolsCompilationService compilationService = new DroolsCompilationService();

        SessionPoolConfig poolConfig = new SessionPoolConfig();
        poolConfig.setEnablePooling(false);
        poolConfig.setMaxPoolSize(1);

        KieSessionFactory sessionFactory = new KieSessionFactory();
        KieSessionPool sessionPool = new KieSessionPool(sessionFactory, poolConfig);
        KieSessionManager sessionManager = new KieSessionManager(sessionPool, poolConfig, sessionFactory);
        sessionManager.initializeSessionFactory();
        sessionManager.cacheContainer("hash-metrics-test", allowContainer);

        service = new SimulateEvaluationService(
                registry,
                compilationService,
                new FactPreparationService(),
                sessionManager,
                Mockito.mock(QuotaCounterService.class),
                analyticsMetrics
        );
    }

    // -----------------------------------------------------------------------
    // Test: ALLOW verdict → rule_fires_total{verdict=ALLOW} incremented
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("ALLOW evaluation path")
    class AllowPath {

        @Test
        @DisplayName("rule_fires_total{verdict=ALLOW} incremented after ALLOW evaluation")
        void allowEval_firesCounterIncremented() {
            EvaluateRuleResponse response = service.evaluate(
                    List.of(RULE_ID),
                    allowFacts(),
                    false
            );

            assertThat(response.getVerdict()).isEqualTo("ALLOW");

            double firesTotalAllow = meterRegistry.find("rule_fires_total")
                    .tag("rule_id", RULE_ID)
                    .tag("verdict", "ALLOW")
                    .counter()
                    .count();

            assertThat(firesTotalAllow)
                    .as("rule_fires_total{verdict=ALLOW} should be 1 after one ALLOW evaluation")
                    .isEqualTo(1.0);
        }

        @Test
        @DisplayName("rule_evaluation_duration_seconds histogram observed after ALLOW")
        void allowEval_histogramObserved() {
            service.evaluate(List.of(RULE_ID), allowFacts(), false);

            Timer timer = meterRegistry.find("rule_evaluation_duration_seconds")
                    .tag("rule_id", RULE_ID)
                    .timer();

            assertThat(timer).as("Timer must be registered").isNotNull();
            assertThat(timer.count())
                    .as("Timer should record exactly 1 observation")
                    .isEqualTo(1L);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS))
                    .as("Timer total time should be > 0")
                    .isGreaterThan(0.0);
        }
    }

    // -----------------------------------------------------------------------
    // Test: DENY verdict → rule_fires_total{verdict=DENY} + rule_rejects_total
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("DENY evaluation path")
    class DenyPath {

        @Test
        @DisplayName("rule_fires_total{verdict=DENY} incremented after DENY evaluation")
        void denyEval_firesCounterDeny() {
            EvaluateRuleResponse response = service.evaluate(
                    List.of(RULE_ID),
                    denyFacts(),
                    false
            );

            assertThat(response.getVerdict()).isEqualTo("DENY");

            double firesTotalDeny = meterRegistry.find("rule_fires_total")
                    .tag("rule_id", RULE_ID)
                    .tag("verdict", "DENY")
                    .counter()
                    .count();

            assertThat(firesTotalDeny)
                    .as("rule_fires_total{verdict=DENY} should be 1 after one DENY evaluation")
                    .isEqualTo(1.0);
        }

        @Test
        @DisplayName("rule_rejects_total{reason_code=CONDITION_NOT_MET} incremented after DENY")
        void denyEval_rejectsCounterIncremented() {
            service.evaluate(List.of(RULE_ID), denyFacts(), false);

            Counter rejectCounter = meterRegistry.find("rule_rejects_total")
                    .tag("rule_id", RULE_ID)
                    .tag("reason_code", "CONDITION_NOT_MET")
                    .counter();

            assertThat(rejectCounter).as("rule_rejects_total counter must be registered").isNotNull();
            assertThat(rejectCounter.count())
                    .as("rule_rejects_total{reason_code=CONDITION_NOT_MET} should be 1")
                    .isEqualTo(1.0);
        }

        @Test
        @DisplayName("rule_evaluation_duration_seconds histogram observed after DENY")
        void denyEval_histogramObserved() {
            service.evaluate(List.of(RULE_ID), denyFacts(), false);

            Timer timer = meterRegistry.find("rule_evaluation_duration_seconds")
                    .tag("rule_id", RULE_ID)
                    .timer();

            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1L);
        }
    }

    // -----------------------------------------------------------------------
    // Test: all three metric names are registered (presence check)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Metric name presence — exposed on /actuator/prometheus")
    class MetricPresence {

        @Test
        @DisplayName("All three metric families registered after any evaluation")
        void allThreeMetricNamesRegistered() {
            // Trigger one evaluation to seed metrics
            service.evaluate(List.of(RULE_ID), allowFacts(), false);

            // rule_fires_total
            assertThat(meterRegistry.find("rule_fires_total").counters())
                    .as("rule_fires_total must be registered")
                    .isNotEmpty();

            // rule_evaluation_duration_seconds (Timer maps to _count/_sum/_bucket)
            assertThat(meterRegistry.find("rule_evaluation_duration_seconds").timers())
                    .as("rule_evaluation_duration_seconds must be registered")
                    .isNotEmpty();
        }

        @Test
        @DisplayName("rule_rejects_total registered after DENY evaluation")
        void rejectsMetricRegisteredAfterDeny() {
            service.evaluate(List.of(RULE_ID), denyFacts(), false);

            assertThat(meterRegistry.find("rule_rejects_total").counters())
                    .as("rule_rejects_total must be registered after DENY")
                    .isNotEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // Test: multiple evaluations accumulate correctly
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Accumulation across multiple evaluations")
    class Accumulation {

        @Test
        @DisplayName("Two ALLOW + one DENY → fires counter reflects both verdicts")
        void multipleEvals_countersAccumulate() {
            service.evaluate(List.of(RULE_ID), allowFacts(), false);
            service.evaluate(List.of(RULE_ID), allowFacts(), false);
            service.evaluate(List.of(RULE_ID), denyFacts(), false);

            double allowCount = meterRegistry.find("rule_fires_total")
                    .tag("rule_id", RULE_ID)
                    .tag("verdict", "ALLOW")
                    .counter()
                    .count();

            double denyCount = meterRegistry.find("rule_fires_total")
                    .tag("rule_id", RULE_ID)
                    .tag("verdict", "DENY")
                    .counter()
                    .count();

            assertThat(allowCount).as("ALLOW fires should be 2").isEqualTo(2.0);
            assertThat(denyCount).as("DENY fires should be 1").isEqualTo(1.0);
        }

        @Test
        @DisplayName("Timer records all evaluations: count = total calls")
        void multipleEvals_histogramCountMatchesEvals() {
            service.evaluate(List.of(RULE_ID), allowFacts(), false);
            service.evaluate(List.of(RULE_ID), denyFacts(), false);
            service.evaluate(List.of(RULE_ID), allowFacts(), false);

            Timer timer = meterRegistry.find("rule_evaluation_duration_seconds")
                    .tag("rule_id", RULE_ID)
                    .timer();

            assertThat(timer).isNotNull();
            assertThat(timer.count())
                    .as("Timer count should equal 3 (total evaluations)")
                    .isEqualTo(3L);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Map<String, Object> allowFacts() {
        Customer customer = new Customer("c1", Set.of("seg_vip"));
        Order order = new Order("o1");
        order.setTotal(BigDecimal.valueOf(600_000));
        order.setCurrency("VND");
        return Map.of("customer", customer, "order", order);
    }

    private Map<String, Object> denyFacts() {
        Customer customer = new Customer("c1", Set.of("seg_vip"));
        Order order = new Order("o1");
        order.setTotal(BigDecimal.valueOf(100)); // below threshold → DENY
        order.setCurrency("VND");
        return Map.of("customer", customer, "order", order);
    }

    private KieContainer compileToContainer(String drl) {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/rules/metrics-test.drl", drl);
        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if (kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            fail("DRL compile error:\n" + kb.getResults().getMessages());
        }
        KieModule module = kb.getKieModule();
        return ks.newKieContainer(module.getReleaseId());
    }
}
