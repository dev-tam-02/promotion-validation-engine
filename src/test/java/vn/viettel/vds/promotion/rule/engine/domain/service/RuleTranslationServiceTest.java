package vn.viettel.vds.promotion.rule.engine.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslatorRegistry;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RuleTranslationService — DRL generation")
class RuleTranslationServiceTest {

    private RuleTranslationService service;

    @BeforeEach
    void setUp() {
        // Wire real operator translators (no mocks — testing DRL output accuracy)
        List<vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator> translators = List.of(
                new OrderTotalGteOperatorTranslator(),
                new CustomerSegmentInOperatorTranslator(),
                new OrderItemCategoryInOperatorTranslator(),
                new OrderItemBrandInOperatorTranslator(),
                new BudgetRedemptionsTotalLteOperatorTranslator(),
                new OrderItemsCategorySumGteOperatorTranslator(),
                new OrderItemsMatchingCountGteOperatorTranslator(),
                new OrderItemsAveragePriceGteOperatorTranslator()
        );
        OperatorTranslatorRegistry registry = new OperatorTranslatorRegistry(translators);
        service = new RuleTranslationService(registry);
    }

    private List<Map<String, Object>> singleConditionNodes(String operatorName, Map<String, Object> params) {
        return List.of(
                Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL",
                        "children", List.of("c1")),
                Map.of("id", "c1", "type", "COND", "operatorName", operatorName,
                        "operatorVersion", 1, "params", params, "reasonCode", "TEST_REASON")
        );
    }

    @Nested
    @DisplayName("DRL header")
    class DrlHeader {

        @Test
        @DisplayName("should NOT contain System.out.println")
        void noSystemOut() {
            List<Map<String, Object>> nodes = singleConditionNodes("order.total.gte", Map.of("amount", "100"));
            String drl = service.translateToDrl(nodes, false);

            assertFalse(drl.contains("System.out.println"), "Generated DRL must not contain System.out.println");
        }

        @Test
        @DisplayName("should NOT contain usageService global")
        void noUsageServiceGlobal() {
            List<Map<String, Object>> nodes = singleConditionNodes("order.total.gte", Map.of("amount", "100"));
            String drl = service.translateToDrl(nodes, false);

            assertFalse(drl.contains("usageService"), "Generated DRL must not declare usageService global");
        }

        @Test
        @DisplayName("should import LimitsCtx")
        void importsLimitsCtx() {
            List<Map<String, Object>> nodes = singleConditionNodes("order.total.gte", Map.of("amount", "100"));
            String drl = service.translateToDrl(nodes, false);

            assertTrue(drl.contains("import vn.viettel.vds.promotion.rule.engine.domain.model.LimitsCtx"),
                    "DRL must import LimitsCtx for budget operators");
        }

        @Test
        @DisplayName("should declare TemporalAllowed when no temporal policy")
        void declaresTemporalAllowedWithoutPolicy() {
            List<Map<String, Object>> nodes = singleConditionNodes("order.total.gte", Map.of("amount", "100"));
            String drl = service.translateToDrl(nodes, false);

            assertTrue(drl.contains("declare TemporalAllowed"));
            assertTrue(drl.contains("insert_temporal_allowed"));
        }

        @Test
        @DisplayName("should NOT declare TemporalAllowed when temporal policy present")
        void noTemporalAllowedWithPolicy() {
            List<Map<String, Object>> nodes = singleConditionNodes("order.total.gte", Map.of("amount", "100"));
            String drl = service.translateToDrl(nodes, true);

            assertFalse(drl.contains("declare TemporalAllowed"));
        }
    }

    @Nested
    @DisplayName("GROUP logic")
    class GroupLogic {

        @Test
        @DisplayName("ALL logic — AND conditions")
        void allLogic() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL",
                            "children", List.of("c1", "c2")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100"), "reasonCode", "MIN_ORDER"),
                    Map.of("id", "c2", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("VIP")), "reasonCode", "NOT_VIP")
            );
            String drl = service.translateToDrl(nodes);

            // ALL = no OR, just sequential conditions
            assertTrue(drl.contains("Order(total != null"));
            assertTrue(drl.contains("Customer(segments"));
            assertFalse(drl.contains("    or\n"));
        }

        @Test
        @DisplayName("ANY logic — OR conditions")
        void anyLogic() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ANY",
                            "children", List.of("c1", "c2")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100"), "reasonCode", "R1"),
                    Map.of("id", "c2", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("VIP")), "reasonCode", "R2")
            );
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("or"), "ANY logic must use 'or' keyword");
        }

        @Test
        @DisplayName("NONE logic — NOT (OR conditions)")
        void noneLogic() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "NONE",
                            "children", List.of("c1")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("BLOCKED")), "reasonCode", "BLOCKED")
            );
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("not ("), "NONE logic must use 'not (' wrapper");
        }

        @Test
        @DisplayName("XOR logic — exactly one condition")
        void xorLogic() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "XOR",
                            "children", List.of("c1", "c2")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100"), "reasonCode", "R1"),
                    Map.of("id", "c2", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "500"), "reasonCode", "R2")
            );
            String drl = service.translateToDrl(nodes);

            // XOR generates: (A and not(B)) or (not(A) and B)
            assertTrue(drl.contains("not ("), "XOR must negate non-matching conditions");
            assertTrue(drl.contains("or"), "XOR must use OR between permutations");
        }
    }

    @Nested
    @DisplayName("Failure tracking rules")
    class FailureTracking {

        @Test
        @DisplayName("generates failure rule per condition with reasonCode")
        void generatesFailureRulesPerCondition() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL",
                            "children", List.of("c1", "c2")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100"), "reasonCode", "MIN_ORDER"),
                    Map.of("id", "c2", "type", "COND", "operatorName", "customer.in_segment",
                            "operatorVersion", 1, "params", Map.of("segments", List.of("VIP")), "reasonCode", "NOT_VIP")
            );
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("failure_tracking_c1"));
            assertTrue(drl.contains("failure_tracking_c2"));
            assertTrue(drl.contains("reasonCodes.add(\"MIN_ORDER\")"));
            assertTrue(drl.contains("reasonCodes.add(\"NOT_VIP\")"));
        }

        @Test
        @DisplayName("skips failure rule for condition without reasonCode")
        void skipsConditionWithoutReasonCode() {
            List<Map<String, Object>> nodes = List.of(
                    Map.of("id", "root", "type", "GROUP", "groupLogic", "ALL",
                            "children", List.of("c1")),
                    Map.of("id", "c1", "type", "COND", "operatorName", "order.total.gte",
                            "operatorVersion", 1, "params", Map.of("amount", "100"))
                    // no reasonCode
            );
            String drl = service.translateToDrl(nodes);

            assertFalse(drl.contains("failure_tracking_c1"));
        }

        @Test
        @DisplayName("overall failure rule sets DENY")
        void overallFailureRule() {
            List<Map<String, Object>> nodes = singleConditionNodes("order.total.gte", Map.of("amount", "100"));
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("promotion_validation_failure"));
            assertTrue(drl.contains("result.setDecision(\"DENY\")"));
            assertTrue(drl.contains("result.setOk(false)"));
        }
    }

    @Nested
    @DisplayName("Main validation rule")
    class MainRule {

        @Test
        @DisplayName("sets ALLOW and inserts RuleMatched on success")
        void allowAndRuleMatched() {
            List<Map<String, Object>> nodes = singleConditionNodes("order.total.gte", Map.of("amount", "100"));
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("result.setDecision(\"ALLOW\")"));
            assertTrue(drl.contains("result.setOk(true)"));
            assertTrue(drl.contains("insert(new RuleMatched())"));
        }

        @Test
        @DisplayName("requires TemporalAllowed() in when clause")
        void requiresTemporalAllowed() {
            List<Map<String, Object>> nodes = singleConditionNodes("order.total.gte", Map.of("amount", "100"));
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("TemporalAllowed()"));
        }
    }

    @Nested
    @DisplayName("OrderItem operators — scoped to order")
    class OrderItemScoping {

        @Test
        @DisplayName("category.in uses 'from $order.items'")
        void categoryInScoped() {
            List<Map<String, Object>> nodes = singleConditionNodes(
                    "order.item.category.in", Map.of("categories", List.of("electronics")));
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("from $order.getItems()"),
                    "OrderItem category.in must be scoped to $order.getItems()");
        }

        @Test
        @DisplayName("brand.in uses 'from $order.getItems()'")
        void brandInScoped() {
            List<Map<String, Object>> nodes = singleConditionNodes(
                    "order.item.brand.in", Map.of("brands", List.of("Nike")));
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("from $order.getItems()"));
        }
    }

    // ============= Helper Methods =============

    @Nested
    @DisplayName("Accumulate operators")
    class AccumulateOperators {

        @Test
        @DisplayName("category sum gte uses accumulate")
        void categorySumGte() {
            List<Map<String, Object>> nodes = singleConditionNodes(
                    "order.items.category.sum.gte",
                    Map.of("category", "electronics", "minTotal", 500000));
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("accumulate"), "Must use Drools accumulate");
            assertTrue(drl.contains("sum("), "Must use sum function");
            assertTrue(drl.contains("\"electronics\""));
        }

        @Test
        @DisplayName("matching count gte uses accumulate count")
        void matchingCountGte() {
            List<Map<String, Object>> nodes = singleConditionNodes(
                    "order.items.matching.count.gte",
                    Map.of("minCount", 3, "categories", List.of("food", "drink")));
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("accumulate"));
            assertTrue(drl.contains("count("));
            assertTrue(drl.contains("intValue >= 3"));
        }

        @Test
        @DisplayName("average price gte uses accumulate average")
        void averagePriceGte() {
            List<Map<String, Object>> nodes = singleConditionNodes(
                    "order.items.average.price.gte",
                    Map.of("minAveragePrice", 100000));
            String drl = service.translateToDrl(nodes);

            assertTrue(drl.contains("accumulate"));
            assertTrue(drl.contains("average("));
        }
    }
}
