package vn.viettel.vds.promotion.rule.engine.domain.service;

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
import vn.viettel.vds.promotion.rule.engine.domain.model.*;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslatorRegistry;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test: build rule nodes → generate DRL → compile with Drools → execute → verify decision.
 * No mocks — uses real Drools engine. Tests the FULL pipeline.
 */
@DisplayName("Rule Compile & Execute — E2E flow")
class RuleCompileAndExecuteFlowTest {

    private RuleTranslationService translationService;

    @BeforeEach
    void setUp() {
        List<vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator> translators = List.of(
                new OrderTotalGteOperatorTranslator(),
                new OrderTotalLteOperatorTranslator(),
                new CustomerSegmentInOperatorTranslator(),
                new OrderItemCategoryInOperatorTranslator(),
                new OrderItemBrandInOperatorTranslator(),
                new BudgetRedemptionsTotalLteOperatorTranslator(),
                new BudgetRedemptionsPerDayLteOperatorTranslator(),
                new OrderItemsCategorySumGteOperatorTranslator(),
                new OrderItemsMatchingCountGteOperatorTranslator(),
                new OrderItemsAveragePriceGteOperatorTranslator()
        );
        OperatorTranslatorRegistry registry = new OperatorTranslatorRegistry(translators);
        translationService = new RuleTranslationService(registry);
    }

    // ======== Helpers ========

    private KieContainer compileAndGetContainer(String drl) {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/rules/test-rule.drl", drl);
        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();

        if (kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
            fail("DRL compilation failed:\n" + kb.getResults().getMessages());
        }

        KieModule module = kb.getKieModule();
        return ks.newKieContainer(module.getReleaseId());
    }

    private ExecutionResult executeRule(String drl, Customer customer, Order order) {
        KieContainer container = compileAndGetContainer(drl);
        StatelessKieSession session = container.newStatelessKieSession();

        ValidationResult result = new ValidationResult();
        List<String> reasonCodes = new ArrayList<>();

        session.setGlobal("result", result);
        session.setGlobal("reasonCodes", reasonCodes);

        List<Object> facts = new ArrayList<>();
        facts.add(customer);
        facts.add(order);
        if (order.getItems() != null) {
            facts.addAll(order.getItems());
        }
        // Always add LimitsCtx
        facts.add(new LimitsCtx());

        session.execute(facts);

        return new ExecutionResult(
                result.getDecision(),
                Boolean.TRUE.equals(result.getOk()),
                result.getReasonCodes() != null ? result.getReasonCodes() : reasonCodes
        );
    }

    private Customer customerWithSegments(String... segments) {
        Customer c = new Customer();
        c.setId("cust-001");
        c.setSegments(new HashSet<>(Arrays.asList(segments)));
        return c;
    }

    private Order orderWithTotal(BigDecimal total) {
        Order o = new Order();
        o.setId("ord-001");
        o.setCurrency("VND");
        o.setTotal(total);
        o.setItems(List.of());
        return o;
    }

    private Order orderWithItems(BigDecimal total, List<OrderItem> items) {
        Order o = new Order();
        o.setId("ord-001");
        o.setCurrency("VND");
        o.setTotal(total);
        o.setItems(items);
        return o;
    }

    private OrderItem item(String category, String brand, double price, int qty) {
        OrderItem i = new OrderItem();
        i.setProductId("prod-" + category);
        i.setCategory(category);
        i.setBrand(brand);
        i.setPrice(price);
        i.setQuantity(qty);
        return i;
    }

    // Sonar rules S100/S1186/S1172 are false positives on Java records (older sonar-java plugins
    // analyze record components/canonical constructor as regular methods with unused params).
    @SuppressWarnings({"java:S100", "java:S1186", "java:S1172"})
    private record ExecutionResult(String decision, boolean ok, List<String> reasonCodes) { // NOSONAR
        // Empty body intentional — Java record canonical constructor is implicit.
    }

    // ======== Tests ========

    @Nested
    @DisplayName("Simple condition — order.total.gte")
    class SimpleCondition {

        @Test
        @DisplayName("ALLOW when order total meets minimum")
        void allowWhenMeetsMinimum() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("c1")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100000"), "reasonCode", "MIN_ORDER")
            );
            String drl = translationService.translateToDrl(nodes);
            ExecutionResult r = executeRule(drl, customerWithSegments(), orderWithTotal(BigDecimal.valueOf(200000)));

            assertEquals("ALLOW", r.decision());
            assertTrue(r.ok());
        }

        @Test
        @DisplayName("DENY when order total below minimum")
        void denyWhenBelowMinimum() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("c1")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100000"), "reasonCode", "MIN_ORDER")
            );
            String drl = translationService.translateToDrl(nodes);
            ExecutionResult r = executeRule(drl, customerWithSegments(), orderWithTotal(BigDecimal.valueOf(50000)));

            assertEquals("DENY", r.decision());
            assertFalse(r.ok());
            assertTrue(r.reasonCodes().contains("MIN_ORDER"));
        }
    }

    @Nested
    @DisplayName("ALL logic — multiple conditions")
    class AllLogicMultiple {

        @Test
        @DisplayName("ALLOW when ALL conditions met")
        void allowWhenAllMet() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("c1", "c2")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100000"), "reasonCode", "MIN_ORDER"),
                    Map.of("id", "c2", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("VIP")), "reasonCode", "NOT_VIP")
            );
            String drl = translationService.translateToDrl(nodes);

            ExecutionResult r = executeRule(drl,
                    customerWithSegments("VIP"),
                    orderWithTotal(BigDecimal.valueOf(200000)));

            assertEquals("ALLOW", r.decision());
        }

        @Test
        @DisplayName("DENY with multiple reason codes when multiple conditions fail")
        void denyWithMultipleReasons() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("c1", "c2")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100000"), "reasonCode", "MIN_ORDER"),
                    Map.of("id", "c2", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("VIP")), "reasonCode", "NOT_VIP")
            );
            String drl = translationService.translateToDrl(nodes);

            ExecutionResult r = executeRule(drl,
                    customerWithSegments("REGULAR"),
                    orderWithTotal(BigDecimal.valueOf(50000)));

            assertEquals("DENY", r.decision());
            assertTrue(r.reasonCodes().contains("MIN_ORDER"), "Should report MIN_ORDER failure");
            assertTrue(r.reasonCodes().contains("NOT_VIP"), "Should report NOT_VIP failure");
        }
    }

    @Nested
    @DisplayName("ANY logic — at least one condition")
    class AnyLogic {

        @Test
        @DisplayName("ALLOW when at least one condition met")
        void allowWhenOneMet() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ANY", "children", List.of("c1", "c2")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "1000000"), "reasonCode", "R1"),
                    Map.of("id", "c2", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("VIP")), "reasonCode", "R2")
            );
            String drl = translationService.translateToDrl(nodes);

            // Order total too low, but customer IS VIP → should ALLOW
            ExecutionResult r = executeRule(drl,
                    customerWithSegments("VIP"),
                    orderWithTotal(BigDecimal.valueOf(50000)));

            assertEquals("ALLOW", r.decision());
        }
    }

    @Nested
    @DisplayName("OrderItem conditions — scoped to order")
    class OrderItemConditions {

        @Test
        @DisplayName("ALLOW when order contains item in target category")
        void allowWithMatchingCategory() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("c1")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.item.category.in",
                            "operatorVersion", 1, "params", Map.of("categories", List.of("electronics", "fashion")),
                            "reasonCode", "WRONG_CATEGORY")
            );
            String drl = translationService.translateToDrl(nodes);

            Order order = orderWithItems(BigDecimal.valueOf(500000), List.of(
                    item("electronics", "Sony", 300000, 1),
                    item("food", "ABC", 200000, 1)
            ));
            ExecutionResult r = executeRule(drl, customerWithSegments(), order);

            assertEquals("ALLOW", r.decision());
        }

        @Test
        @DisplayName("DENY when no item matches target category")
        void denyWithNoMatchingCategory() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("c1")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.item.category.in",
                            "operatorVersion", 1, "params", Map.of("categories", List.of("electronics")),
                            "reasonCode", "WRONG_CATEGORY")
            );
            String drl = translationService.translateToDrl(nodes);

            Order order = orderWithItems(BigDecimal.valueOf(500000), List.of(
                    item("food", "ABC", 500000, 1)
            ));
            ExecutionResult r = executeRule(drl, customerWithSegments(), order);

            assertEquals("DENY", r.decision());
            assertTrue(r.reasonCodes().contains("WRONG_CATEGORY"));
        }
    }

    @Nested
    @DisplayName("Nested GROUP — ALL containing ANY")
    class NestedGroups {

        @Test
        @DisplayName("ALLOW with nested ALL > ANY structure")
        void nestedAllAny() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("g1", "c3")),
                    Map.of("id", "g1", "type", "GROUP", "groupLogic", "ANY", "children", List.of("c1", "c2")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("VIP")), "reasonCode", "R1"),
                    Map.of("id", "c2", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("GOLD")), "reasonCode", "R2"),
                    Map.of("id", "c3", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100000"), "reasonCode", "MIN_ORDER")
            );
            String drl = translationService.translateToDrl(nodes);

            // GOLD segment (matches ANY) + order 200k (matches order.total.gte) → ALLOW
            ExecutionResult r = executeRule(drl,
                    customerWithSegments("GOLD"),
                    orderWithTotal(BigDecimal.valueOf(200000)));

            assertEquals("ALLOW", r.decision());
        }
    }

    @Nested
    @DisplayName("DRL quality checks")
    class DrlQuality {

        @Test
        @DisplayName("generated DRL compiles without errors")
        void compilesSuccessfully() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("c1", "c2")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "50000"), "reasonCode", "R1"),
                    Map.of("id", "c2", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("VIP")), "reasonCode", "R2")
            );

            String drl = translationService.translateToDrl(nodes);
            assertDoesNotThrow(() -> compileAndGetContainer(drl), "DRL must compile without errors");
        }

        @Test
        @DisplayName("deterministic — same input produces same DRL")
        void deterministic() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL", "children", List.of("c2", "c1")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100"), "reasonCode", "R1"),
                    Map.of("id", "c2", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("VIP")), "reasonCode", "R2")
            );

            String drl1 = translationService.translateToDrl(nodes);
            String drl2 = translationService.translateToDrl(nodes);

            assertEquals(drl1, drl2, "Same input must produce identical DRL");
        }
    }
}
