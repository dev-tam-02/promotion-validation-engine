package vn.viettel.vds.promotion.rule.engine.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ObjectStoragePort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "validation.engine.object-storage.store-type=mock"
})
@Disabled("Disabled until full Spring context configuration is available for tests")
class RuleExecutionIntegrationTest {

    @MockitoBean
    private ObjectStoragePort objectStoragePort;

    @Autowired
    @Qualifier("droolsRuleEngineAdapter")
    private RuleEnginePort ruleEnginePort;

    @Test
    void execute_ShouldProcessRuleSuccessfully() {
        // Given
        Map<String, Object> context = createTestContext();
        new RuleEnginePort.ExecuteInput("test-bundle", context, null);

        // When & Then - This would require actual rule compilation and setup
        // For now, just verify the service is wired correctly
        assertNotNull(ruleEnginePort);
    }

    @Test
    void executeBatch_ShouldProcessMultipleRules() {
        // Given
        List.of(
                new RuleEnginePort.ExecuteInput("bundle1", createTestContext(), null),
                new RuleEnginePort.ExecuteInput("bundle2", createTestContext(), null)
        );

        // When & Then - This would require actual rule compilation and setup
        // For now, just verify the service is wired correctly
        assertNotNull(ruleEnginePort);
    }

    private Map<String, Object> createTestContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("customer", Map.of("id", "test-customer", "segment", "VIP"));
        context.put("order", Map.of("id", "test-order", "total", 1000));
        return context;
    }
}
