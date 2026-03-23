package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.rule.engine.domain.model.Customer;
import vn.viettel.vds.promotion.rule.engine.domain.model.LimitsCtx;
import vn.viettel.vds.promotion.rule.engine.domain.model.Order;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FactPreparationService — fact conversion")
class FactPreparationServiceTest {

    private FactPreparationService service;

    @BeforeEach
    void setUp() {
        service = new FactPreparationService();
    }

    @Nested
    @DisplayName("Customer facts")
    class CustomerFacts {

        @Test
        @DisplayName("converts customer map to Customer domain object")
        void convertsCustomerMap() {
            Map<String, Object> context = Map.of(
                    "customer", Map.of("id", "cust-001", "tier", "GOLD",
                            "name", "Test", "email", "test@test.com")
            );

            List<Object> facts = service.prepareFacts(context);

            Customer customer = facts.stream()
                    .filter(Customer.class::isInstance)
                    .map(Customer.class::cast)
                    .findFirst().orElse(null);

            assertNotNull(customer);
            assertEquals("cust-001", customer.getId());
            assertEquals("GOLD", customer.getLoyaltyTier());
        }

        @Test
        @DisplayName("skips customer when null")
        void skipsNullCustomer() {
            Map<String, Object> context = new HashMap<>();
            context.put("customer", null);

            List<Object> facts = service.prepareFacts(context);

            boolean hasCustomer = facts.stream().anyMatch(Customer.class::isInstance);
            assertFalse(hasCustomer);
        }
    }

    @Nested
    @DisplayName("Order facts")
    class OrderFacts {

        @Test
        @DisplayName("converts order map to Order with items")
        void convertsOrderWithItems() {
            Map<String, Object> context = Map.of(
                    "order", Map.of(
                            "id", "ord-001",
                            "currency", "VND",
                            "total", 500000,
                            "items", List.of(
                                    Map.of("productId", "prod-1", "skuId", "sku-1",
                                            "price", 200000, "quantity", 2, "category", "electronics")
                            )
                    )
            );

            List<Object> facts = service.prepareFacts(context);

            Order order = facts.stream()
                    .filter(Order.class::isInstance)
                    .map(Order.class::cast)
                    .findFirst().orElse(null);

            assertNotNull(order);
            assertEquals("ord-001", order.getId());
            assertEquals("VND", order.getCurrency());
            assertEquals(0, BigDecimal.valueOf(500000).compareTo(order.getTotal()));
            assertNotNull(order.getItems());
            assertEquals(1, order.getItems().size());
            assertEquals("electronics", order.getItems().get(0).getCategory());
        }
    }

    @Nested
    @DisplayName("LimitsCtx facts")
    class LimitsFacts {

        @Test
        @DisplayName("populates LimitsCtx from execution context limits map")
        void populatesLimitsFromMap() {
            Map<String, Object> limits = new HashMap<>();
            limits.put("totalRedemptions", 42);
            limits.put("redemptionsPerDay", 5);
            limits.put("perCustomerPerDay", 2);
            limits.put("maxTotalRedemptions", 1000);
            limits.put("maxPerCustomerPerDay", 3);
            limits.put("totalDiscountedAmount", 500000.0);
            limits.put("maxDiscountedAmount", 10000000.0);

            Map<String, Object> execCtx = new HashMap<>();
            execCtx.put("limits", limits);

            Map<String, Object> context = Map.of("executionContext", execCtx);

            List<Object> facts = service.prepareFacts(context);

            LimitsCtx limitsCtx = facts.stream()
                    .filter(LimitsCtx.class::isInstance)
                    .map(LimitsCtx.class::cast)
                    .findFirst().orElse(null);

            assertNotNull(limitsCtx, "LimitsCtx must be present in facts");
            assertEquals(42, limitsCtx.getTotalRedemptions());
            assertEquals(5, limitsCtx.getRedemptionsPerDay());
            assertEquals(2, limitsCtx.getPerCustomerPerDay());
            assertEquals(1000, limitsCtx.getMaxTotalRedemptions());
            assertEquals(3, limitsCtx.getMaxPerCustomerPerDay());
            assertTrue(limitsCtx.getTotalDiscountedAmount().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("inserts empty LimitsCtx when no limits in context")
        void emptyLimitsWhenMissing() {
            Map<String, Object> context = Map.of(
                    "executionContext", Map.of("timestamp", 1234567890L)
            );

            List<Object> facts = service.prepareFacts(context);

            LimitsCtx limitsCtx = facts.stream()
                    .filter(LimitsCtx.class::isInstance)
                    .map(LimitsCtx.class::cast)
                    .findFirst().orElse(null);

            assertNotNull(limitsCtx, "Empty LimitsCtx must be inserted as default");
            assertEquals(0, limitsCtx.getTotalRedemptions());
            assertTrue(limitsCtx.isWithinTotalRedemptionsLimit(), "Empty limits should pass all checks");
        }

        @Test
        @DisplayName("inserts empty LimitsCtx when no executionContext at all")
        void emptyLimitsWhenNoExecContext() {
            Map<String, Object> context = Map.of(
                    "customer", Map.of("id", "c1")
            );

            List<Object> facts = service.prepareFacts(context);

            LimitsCtx limitsCtx = facts.stream()
                    .filter(LimitsCtx.class::isInstance)
                    .map(LimitsCtx.class::cast)
                    .findFirst().orElse(null);

            assertNotNull(limitsCtx, "LimitsCtx must always be present");
        }

        @Test
        @DisplayName("LimitsCtx helper methods work correctly")
        void helperMethodsWork() {
            Map<String, Object> limits = Map.of(
                    "redemptionsPerDay", 10,
                    "maxRedemptionsPerDay", 5
            );
            Map<String, Object> context = Map.of(
                    "executionContext", Map.of("limits", limits)
            );

            List<Object> facts = service.prepareFacts(context);
            LimitsCtx limitsCtx = facts.stream()
                    .filter(LimitsCtx.class::isInstance)
                    .map(LimitsCtx.class::cast)
                    .findFirst().orElseThrow();

            assertFalse(limitsCtx.isWithinDailyRedemptionsLimit(),
                    "10 redemptions should exceed limit of 5");
        }
    }
}
