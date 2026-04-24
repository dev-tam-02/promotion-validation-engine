package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.rule.engine.application.port.in.RegisterDrlUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleRegistryPort;
import vn.viettel.vds.promotion.rule.engine.domain.model.RegisteredRule;
import vn.viettel.vds.promotion.rule.engine.domain.service.execution.IncrementalKieContainerService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DrlRegistrationService}.
 *
 * <p>After Task 09 V5 the service no longer directly compiles DRL or calls
 * {@code warmupBundle}. Instead it delegates to {@link IncrementalKieContainerService}
 * which is mocked here to isolate the application-layer orchestration logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DrlRegistrationService Tests — V5 incremental path")
class DrlRegistrationServiceTest {

    private static final String RULE_ID = "rule-promo-001";
    private static final String VALID_DRL = """
            package rules;
            rule "test"
            when eval(true)
            then end
            """;
    private static final String BUNDLE_HASH = "sha256:abc123def456";

    @Mock
    private RuleRegistryPort ruleRegistry;

    @Mock
    private IncrementalKieContainerService incrementalKieSvc;

    @Captor
    private ArgumentCaptor<RegisteredRule> savedRuleCaptor;

    private DrlRegistrationService sut;

    @BeforeEach
    void setUp() {
        sut = new DrlRegistrationService(ruleRegistry, incrementalKieSvc);
    }

    // ------------------------------------------------------------------
    // register()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Should delegate to incremental service, save to registry and return bundleHash")
        void shouldRegisterNewRule() {
            // Given
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.empty());
            when(ruleRegistry.findAll()).thenReturn(Collections.emptyList());
            when(incrementalKieSvc.registerOrUpdate(eq(RULE_ID), eq(VALID_DRL), any(Map.class)))
                    .thenReturn(BUNDLE_HASH);

            // When
            RegisterDrlUseCase.RegisterRuleResult result = sut.register(RULE_ID, VALID_DRL);

            // Then
            assertThat(result.ruleId()).isEqualTo(RULE_ID);
            assertThat(result.bundleHash()).isEqualTo(BUNDLE_HASH);

            verify(incrementalKieSvc).registerOrUpdate(eq(RULE_ID), eq(VALID_DRL), any(Map.class));
            verify(ruleRegistry).save(savedRuleCaptor.capture());

            RegisteredRule saved = savedRuleCaptor.getValue();
            assertThat(saved.getRuleId()).isEqualTo(RULE_ID);
            assertThat(saved.getDrl()).isEqualTo(VALID_DRL);
            assertThat(saved.getBundleHash()).isEqualTo(BUNDLE_HASH);
            assertThat(saved.getRegisteredAt()).isNotNull();
        }

        @Test
        @DisplayName("Should return existing bundleHash without recompiling when same (id, drl) submitted")
        void shouldBeIdempotentForSameDrl() {
            // Given
            RegisteredRule existing = new RegisteredRule(
                    RULE_ID, VALID_DRL, BUNDLE_HASH, Instant.now(), Instant.now());
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.of(existing));

            // When
            RegisterDrlUseCase.RegisterRuleResult result = sut.register(RULE_ID, VALID_DRL);

            // Then
            assertThat(result.ruleId()).isEqualTo(RULE_ID);
            assertThat(result.bundleHash()).isEqualTo(BUNDLE_HASH);

            // No incremental update, no save
            verifyNoInteractions(incrementalKieSvc);
            verify(ruleRegistry, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DrlConflictException when same id submitted with different DRL")
        void shouldThrowConflictForDifferentDrl() {
            // Given
            RegisteredRule existing = new RegisteredRule(
                    RULE_ID, "old drl content", BUNDLE_HASH, Instant.now(), Instant.now());
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.of(existing));

            // When / Then
            assertThatThrownBy(() -> sut.register(RULE_ID, VALID_DRL))
                    .isInstanceOf(RegisterDrlUseCase.DrlConflictException.class)
                    .hasMessageContaining(RULE_ID);

            verifyNoInteractions(incrementalKieSvc);
        }

        @Test
        @DisplayName("Should throw DrlCompileException when incremental service fails compilation")
        void shouldThrowCompileExceptionForInvalidDrl() {
            // Given
            String badDrl = "this is not valid DRL !!!";
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.empty());
            when(ruleRegistry.findAll()).thenReturn(Collections.emptyList());
            when(incrementalKieSvc.registerOrUpdate(anyString(), eq(badDrl), any(Map.class)))
                    .thenThrow(new IncrementalKieContainerService.IncrementalCompileException(
                            "DRL compilation failed", List.of("[ERROR] syntax error near '!!!'")
                    ));

            // When / Then
            assertThatThrownBy(() -> sut.register(RULE_ID, badDrl))
                    .isInstanceOf(RegisterDrlUseCase.DrlCompileException.class)
                    .hasMessageContaining("DRL compilation failed");

            verify(ruleRegistry, never()).save(any());
        }
    }

    // ------------------------------------------------------------------
    // update()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Should delegate to incremental service, update registry and return new bundleHash")
        void shouldUpdateExistingRule() {
            // Given
            String newDrl = "package rules; rule \"updated\" when eval(true) then end";
            String newHash = "sha256:newHash999";

            RegisteredRule existing = new RegisteredRule(
                    RULE_ID, VALID_DRL, BUNDLE_HASH, Instant.parse("2024-01-01T00:00:00Z"), Instant.now());
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.of(existing));
            when(ruleRegistry.findAll()).thenReturn(List.of(existing));
            when(incrementalKieSvc.registerOrUpdate(eq(RULE_ID), eq(newDrl), any(Map.class)))
                    .thenReturn(newHash);

            // When
            RegisterDrlUseCase.RegisterRuleResult result = sut.update(RULE_ID, newDrl);

            // Then
            assertThat(result.ruleId()).isEqualTo(RULE_ID);
            assertThat(result.bundleHash()).isEqualTo(newHash);

            verify(incrementalKieSvc).registerOrUpdate(eq(RULE_ID), eq(newDrl), any(Map.class));
            verify(ruleRegistry).save(savedRuleCaptor.capture());

            RegisteredRule saved = savedRuleCaptor.getValue();
            assertThat(saved.getDrl()).isEqualTo(newDrl);
            assertThat(saved.getBundleHash()).isEqualTo(newHash);
            // registeredAt preserved from original
            assertThat(saved.getRegisteredAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        }

        @Test
        @DisplayName("Should throw RuleNotFoundException when rule does not exist")
        void shouldThrowNotFoundForUnknownRule() {
            // Given
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> sut.update(RULE_ID, VALID_DRL))
                    .isInstanceOf(RegisterDrlUseCase.RuleNotFoundException.class)
                    .hasMessageContaining(RULE_ID);

            verifyNoInteractions(incrementalKieSvc);
        }

        @Test
        @DisplayName("Should throw DrlCompileException when incremental service fails on new DRL")
        void shouldThrowCompileExceptionForInvalidNewDrl() {
            // Given
            String badDrl = "not valid drl";
            RegisteredRule existing = new RegisteredRule(
                    RULE_ID, VALID_DRL, BUNDLE_HASH, Instant.now(), Instant.now());
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.of(existing));
            when(ruleRegistry.findAll()).thenReturn(List.of(existing));
            when(incrementalKieSvc.registerOrUpdate(anyString(), eq(badDrl), any(Map.class)))
                    .thenThrow(new IncrementalKieContainerService.IncrementalCompileException(
                            "parse error", List.of("[ERROR] unexpected token")));

            // When / Then
            assertThatThrownBy(() -> sut.update(RULE_ID, badDrl))
                    .isInstanceOf(RegisterDrlUseCase.DrlCompileException.class);

            verify(ruleRegistry, never()).save(any());
        }
    }

    // ------------------------------------------------------------------
    // delete()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Should delete existing rule from registry")
        void shouldDeleteExistingRule() {
            // Given
            RegisteredRule existing = new RegisteredRule(
                    RULE_ID, VALID_DRL, BUNDLE_HASH, Instant.now(), Instant.now());
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.of(existing));

            // When
            sut.delete(RULE_ID);

            // Then
            verify(ruleRegistry).deleteById(RULE_ID);
        }

        @Test
        @DisplayName("Should throw RuleNotFoundException when rule does not exist")
        void shouldThrowNotFoundForUnknownRule() {
            // Given
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> sut.delete(RULE_ID))
                    .isInstanceOf(RegisterDrlUseCase.RuleNotFoundException.class)
                    .hasMessageContaining(RULE_ID);

            verify(ruleRegistry, never()).deleteById(any());
        }
    }
}
