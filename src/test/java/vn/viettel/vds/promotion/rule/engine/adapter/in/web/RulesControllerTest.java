package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.RegisterRuleRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.UpdateRuleRequest;
import vn.viettel.vds.promotion.rule.engine.application.port.in.RegisterDrlUseCase;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc tests for {@link RulesController}.
 *
 * <p>Uses standalone setup (no Spring context) to avoid @ResponseWrapper processing and
 * Kafka/DB infrastructure dependencies. Tests verify HTTP status codes and JSON fields.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RulesController Tests")
class RulesControllerTest {

    private static final String RULE_ID = "rule-promo-001";
    private static final String BUNDLE_HASH = "sha256:abc123def456";
    private static final String VALID_DRL = "package rules; rule \"X\" when eval(true) then end";

    @Mock
    private RegisterDrlUseCase registerDrlUseCase;

    @InjectMocks
    private RulesController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Standalone setup: no Spring context, no @ResponseWrapper.
        // Resolve the context-path placeholder to empty string so URL mappings become "/v1/rules".
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addPlaceholderValue("spring.application.context-path", "")
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /v1/rules")
    class PostTests {

        @Test
        @DisplayName("Should return 201 with ruleId and bundleHash on successful registration")
        void shouldReturn201ForValidDrl() throws Exception {
            // Given
            RegisterRuleRequest req = new RegisterRuleRequest(RULE_ID, VALID_DRL);
            when(registerDrlUseCase.register(RULE_ID, VALID_DRL))
                    .thenReturn(new RegisterDrlUseCase.RegisterRuleResult(RULE_ID, BUNDLE_HASH));

            // When / Then
            mockMvc.perform(post("/v1/rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ruleId").value(RULE_ID))
                    .andExpect(jsonPath("$.bundleHash").value(BUNDLE_HASH));
        }

        @Test
        @DisplayName("Should return 400 with DRL_COMPILE_ERROR when DRL is invalid")
        void shouldReturn400ForInvalidDrl() throws Exception {
            // Given
            RegisterRuleRequest req = new RegisterRuleRequest(RULE_ID, "bad drl");
            when(registerDrlUseCase.register(RULE_ID, "bad drl"))
                    .thenThrow(new RegisterDrlUseCase.DrlCompileException(
                            "DRL compilation failed: syntax error", List.of("[ERROR] unexpected token")));

            // When / Then
            mockMvc.perform(post("/v1/rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("DRL_COMPILE_ERROR"))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 409 with RULE_CONFLICT when id exists with different DRL")
        void shouldReturn409ForConflict() throws Exception {
            // Given
            RegisterRuleRequest req = new RegisterRuleRequest(RULE_ID, "different drl");
            when(registerDrlUseCase.register(RULE_ID, "different drl"))
                    .thenThrow(new RegisterDrlUseCase.DrlConflictException(
                            "Rule 'rule-promo-001' already exists with different DRL content."));

            // When / Then
            mockMvc.perform(post("/v1/rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("RULE_CONFLICT"));
        }
    }

    @Nested
    @DisplayName("PUT /v1/rules/{id}")
    class PutTests {

        @Test
        @DisplayName("Should return 200 with updated bundleHash when rule exists")
        void shouldReturn200ForValidUpdate() throws Exception {
            // Given
            String newHash = "sha256:newHash999";
            UpdateRuleRequest req = new UpdateRuleRequest(VALID_DRL);
            when(registerDrlUseCase.update(RULE_ID, VALID_DRL))
                    .thenReturn(new RegisterDrlUseCase.RegisterRuleResult(RULE_ID, newHash));

            // When / Then
            mockMvc.perform(put("/v1/rules/{id}", RULE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ruleId").value(RULE_ID))
                    .andExpect(jsonPath("$.bundleHash").value(newHash));
        }

        @Test
        @DisplayName("Should return 404 with RULE_NOT_FOUND when rule id does not exist")
        void shouldReturn404ForUnknownRule() throws Exception {
            // Given
            UpdateRuleRequest req = new UpdateRuleRequest(VALID_DRL);
            when(registerDrlUseCase.update(RULE_ID, VALID_DRL))
                    .thenThrow(new RegisterDrlUseCase.RuleNotFoundException("Rule not found: " + RULE_ID));

            // When / Then
            mockMvc.perform(put("/v1/rules/{id}", RULE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RULE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 400 with DRL_COMPILE_ERROR when new DRL is invalid")
        void shouldReturn400ForInvalidDrlOnPut() throws Exception {
            // Given
            UpdateRuleRequest req = new UpdateRuleRequest("invalid drl");
            when(registerDrlUseCase.update(RULE_ID, "invalid drl"))
                    .thenThrow(new RegisterDrlUseCase.DrlCompileException(
                            "DRL compilation failed", List.of("[ERROR] parse error")));

            // When / Then
            mockMvc.perform(put("/v1/rules/{id}", RULE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("DRL_COMPILE_ERROR"));
        }
    }

    @Nested
    @DisplayName("DELETE /v1/rules/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 when rule is deleted successfully")
        void shouldReturn204ForSuccessfulDelete() throws Exception {
            // Given
            doNothing().when(registerDrlUseCase).delete(RULE_ID);

            // When / Then
            mockMvc.perform(delete("/v1/rules/{id}", RULE_ID))
                    .andExpect(status().isNoContent());

            verify(registerDrlUseCase).delete(RULE_ID);
        }

        @Test
        @DisplayName("Should return 404 when deleting a non-existent rule")
        void shouldReturn404ForUnknownRule() throws Exception {
            // Given
            doThrow(new RegisterDrlUseCase.RuleNotFoundException("Rule not found: " + RULE_ID))
                    .when(registerDrlUseCase).delete(RULE_ID);

            // When / Then
            mockMvc.perform(delete("/v1/rules/{id}", RULE_ID))
                    .andExpect(status().isNotFound());
        }
    }
}
