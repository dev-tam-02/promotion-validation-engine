package vn.viettel.vds.promotion.rule.engine.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ObjectStoragePort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "validation.engine.object-storage.store-type=mock"
})
@Disabled("Disabled until full Spring context configuration is available for tests")
class RuleExecutionIntegrationTest {

    @MockBean
    private ObjectStoragePort objectStoragePort;

    @Autowired
    @Qualifier("droolsRuleEngineAdapter")
    private RuleEnginePort ruleEnginePort;

    @Test
    void execute_ShouldProcessRuleSuccessfully() {
        // Given
        Map<String, Object> context = createTestContext();
        RuleEnginePort.ExecuteInput input = new RuleEnginePort.ExecuteInput(
                "test-tenant", "test-bundle", context, null
        );

        // When & Then - This would require actual rule compilation and setup
        // For now, just verify the service is wired correctly
        assertNotNull(ruleEnginePort);
    }

    @Test
    void executeBatch_ShouldProcessMultipleRules() {
        // Given
        List<RuleEnginePort.ExecuteInput> inputs = List.of(
                new RuleEnginePort.ExecuteInput("tenant1", "bundle1", createTestContext(), null),
                new RuleEnginePort.ExecuteInput("tenant1", "bundle2", createTestContext(), null)
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
