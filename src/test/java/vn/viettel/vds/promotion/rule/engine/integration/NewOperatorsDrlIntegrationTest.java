package vn.viettel.vds.promotion.rule.engine.integration;

import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import vn.viettel.vds.promotion.rule.engine.domain.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NewOperatorsDrlIntegrationTest {

    @Test
    void testNewOperatorsDrlCompilationAndExecution() {
        // Create DRL content with new operators
        String drlContent = """
                package rules;
                
                import vn.viettel.vds.promotion.rule.engine.domain.model.Customer;
                import vn.viettel.vds.promotion.rule.engine.domain.model.Order;
                import vn.viettel.vds.promotion.rule.engine.domain.model.OrderItem;
                import vn.viettel.vds.promotion.rule.engine.domain.model.Candidate;
                import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;
                
                global ValidationResult result;
                global java.util.List reasonCodes;
                
                rule "Brand_Test_Rule"
                    when
                        $customer: Customer()
                        $order: Order()
                        $candidate: Candidate()
                        exists(OrderItem(brand == "Nike") from $order.items)
                    then
                        result.setDecision("ALLOW");
                        result.setOk(true);
                end
                
                rule "Lifetime_Value_Test_Rule"
                    when
                        $customer: Customer(lifetimeValue >= 1000000)
                        $order: Order()
                        $candidate: Candidate()
                    then
                        result.setDecision("ALLOW");
                        result.setOk(true);
                end
                
                rule "Metadata_Test_Rule"
                    when
                        $customer: Customer(attrs["region"] == "HCM")
                        $order: Order(metadata["channel"] == "MOBILE")
                        $candidate: Candidate()
                    then
                        result.setDecision("ALLOW");
                        result.setOk(true);
                end
                
                rule "Initial_Amount_Test_Rule"
                    when
                        $customer: Customer()
                        $order: Order(initialAmount >= 500000)
                        $candidate: Candidate()
                    then
                        result.setDecision("ALLOW");
                        result.setOk(true);
                end
                """;

        // Test compilation
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write("src/main/resources/rules/test.drl", drlContent);

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        // Check for compilation errors
        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            fail("DRL compilation failed: " + kieBuilder.getResults().getMessages());
        }

        // Test execution
        KieContainer kieContainer = kieServices.newKieContainer(kieBuilder.getKieModule().getReleaseId());
        StatelessKieSession session = kieContainer.newStatelessKieSession();

        // Test Case 1: Brand rule should match
        testBrandRule(session);

        // Test Case 2: Lifetime value rule should match
        testLifetimeValueRule(session);

        // Test Case 3: Metadata rule should match
        testMetadataRule(session);

        // Test Case 4: Initial amount rule should match
        testInitialAmountRule(session);

        // Test Case 5: No rule should match
        testNoRuleMatch(session);
    }

    private void testBrandRule(StatelessKieSession session) {
        // Create test data
        Customer customer = new Customer();
        customer.setId("customer1");
        customer.setLifetimeValue(500000); // Below threshold
        customer.setAttrs(new HashMap<>()); // Initialize empty attrs to prevent NPE

        Order order = new Order();
        order.setId("order1");
        order.setInitialAmount(BigDecimal.valueOf(300000)); // Below threshold
        order.setMetadata(new HashMap<>()); // Initialize empty metadata to prevent NPE

        OrderItem item = new OrderItem();
        item.setBrand("Nike"); // Should match brand rule
        item.setPrice(50000);
        order.setItems(List.of(item));

        Candidate candidate = new Candidate("voucher", "TEST001");
        ValidationResult result = new ValidationResult();
        List<String> reasonCodes = new ArrayList<>();

        // Execute - Don't pass result as fact, only use as global
        session.setGlobal("result", result);
        session.setGlobal("reasonCodes", reasonCodes);
        session.execute(List.of(customer, order, candidate));

        // Verify
        assertEquals("ALLOW", result.getDecision());
        assertTrue(result.getOk());
        System.out.println("Brand rule test: PASSED");
    }

    private void testLifetimeValueRule(StatelessKieSession session) {
        Customer customer = new Customer();
        customer.setId("customer2");
        customer.setLifetimeValue(1500000); // Above threshold
        customer.setAttrs(new HashMap<>()); // Initialize empty attrs to prevent NPE

        Order order = new Order();
        order.setId("order2");
        order.setInitialAmount(BigDecimal.valueOf(300000));
        order.setMetadata(new HashMap<>()); // Initialize empty metadata to prevent NPE

        OrderItem item = new OrderItem();
        item.setBrand("Unknown"); // Won't match brand rule
        order.setItems(List.of(item));

        Candidate candidate = new Candidate("voucher", "TEST002");
        ValidationResult result = new ValidationResult();
        List<String> reasonCodes = new ArrayList<>();

        session.setGlobal("result", result);
        session.setGlobal("reasonCodes", reasonCodes);
        session.execute(List.of(customer, order, candidate));

        assertEquals("ALLOW", result.getDecision());
        assertTrue(result.getOk());
        System.out.println("Lifetime value rule test: PASSED");
    }

    private void testMetadataRule(StatelessKieSession session) {
        Customer customer = new Customer();
        customer.setId("customer3");
        customer.setLifetimeValue(500000); // Below threshold
        customer.setAttrs(Map.of("region", "HCM")); // Should match metadata rule

        Order order = new Order();
        order.setId("order3");
        order.setInitialAmount(BigDecimal.valueOf(300000));
        order.setMetadata(Map.of("channel", "MOBILE")); // Should match metadata rule

        OrderItem item = new OrderItem();
        item.setBrand("Unknown");
        order.setItems(List.of(item));

        Candidate candidate = new Candidate("voucher", "TEST003");
        ValidationResult result = new ValidationResult();
        List<String> reasonCodes = new ArrayList<>();

        session.setGlobal("result", result);
        session.setGlobal("reasonCodes", reasonCodes);
        session.execute(List.of(customer, order, candidate));

        assertEquals("ALLOW", result.getDecision());
        assertTrue(result.getOk());
        System.out.println("Metadata rule test: PASSED");
    }

    private void testInitialAmountRule(StatelessKieSession session) {
        Customer customer = new Customer();
        customer.setId("customer4");
        customer.setLifetimeValue(500000); // Below threshold
        customer.setAttrs(new HashMap<>()); // Initialize empty attrs to prevent NPE

        Order order = new Order();
        order.setId("order4");
        order.setInitialAmount(BigDecimal.valueOf(600000)); // Above threshold
        order.setMetadata(new HashMap<>()); // Initialize empty metadata to prevent NPE

        OrderItem item = new OrderItem();
        item.setBrand("Unknown");
        order.setItems(List.of(item));

        Candidate candidate = new Candidate("voucher", "TEST004");
        ValidationResult result = new ValidationResult();
        List<String> reasonCodes = new ArrayList<>();

        session.setGlobal("result", result);
        session.setGlobal("reasonCodes", reasonCodes);
        session.execute(List.of(customer, order, candidate));

        assertEquals("ALLOW", result.getDecision());
        assertTrue(result.getOk());
        System.out.println("Initial amount rule test: PASSED");
    }

    private void testNoRuleMatch(StatelessKieSession session) {
        Customer customer = new Customer();
        customer.setId("customer5");
        customer.setLifetimeValue(500000); // Below threshold
        customer.setAttrs(new HashMap<>()); // Initialize empty attrs to prevent NPE

        Order order = new Order();
        order.setId("order5");
        order.setInitialAmount(BigDecimal.valueOf(300000)); // Below threshold
        order.setMetadata(new HashMap<>()); // Initialize empty metadata to prevent NPE

        OrderItem item = new OrderItem();
        item.setBrand("Unknown"); // Won't match
        order.setItems(List.of(item));

        Candidate candidate = new Candidate("voucher", "TEST005");
        ValidationResult result = new ValidationResult();
        List<String> reasonCodes = new ArrayList<>();

        session.setGlobal("result", result);
        session.setGlobal("reasonCodes", reasonCodes);
        session.execute(List.of(customer, order, candidate));

        // Without ValidationResult as fact, Default_Failure_Rule can't pattern match on it
        // So we just verify the rule didn't set ALLOW
        assertNotEquals("ALLOW", result.getDecision());
        System.out.println("No rule match test: PASSED");
    }
}
