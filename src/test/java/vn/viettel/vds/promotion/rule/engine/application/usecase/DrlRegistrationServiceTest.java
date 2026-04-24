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
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleRegistryPort;
import vn.viettel.vds.promotion.rule.engine.domain.model.RegisteredRule;
import vn.viettel.vds.promotion.rule.engine.domain.service.DroolsCompilationService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DrlRegistrationService Tests")
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
    private DroolsCompilationService compilationService;

    @Mock
    private RuleRegistryPort ruleRegistry;

    @Mock
    private RuleEnginePort ruleEnginePort;

    @Captor
    private ArgumentCaptor<RegisteredRule> savedRuleCaptor;

    private DrlRegistrationService sut;

    @BeforeEach
    void setUp() {
        sut = new DrlRegistrationService(compilationService, ruleRegistry, ruleEnginePort);
    }

    // ------------------------------------------------------------------
    // register()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Should compile DRL, warm up bundle, save to registry and return bundleHash")
        void shouldRegisterNewRule() {
            // Given
            DroolsCompilationService.CompilationResult compiled =
                    new DroolsCompilationService.CompilationResult(
                            BUNDLE_HASH, new byte[]{1, 2, 3}, 3L, List.of(), "10.1.0");

            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.empty());
            when(compilationService.compileDrl(eq(RULE_ID), eq(1), eq(VALID_DRL)))
                    .thenReturn(compiled);

            // When
            RegisterDrlUseCase.RegisterRuleResult result = sut.register(RULE_ID, VALID_DRL);

            // Then
            assertThat(result.ruleId()).isEqualTo(RULE_ID);
            assertThat(result.bundleHash()).isEqualTo(BUNDLE_HASH);

            verify(ruleEnginePort).warmupBundle(eq(BUNDLE_HASH), any(byte[].class));
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

            // No re-compilation, no warmup, no save
            verifyNoInteractions(compilationService, ruleEnginePort);
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

            verifyNoInteractions(compilationService, ruleEnginePort);
        }

        @Test
        @DisplayName("Should throw DrlCompileException when DRL has syntax errors")
        void shouldThrowCompileExceptionForInvalidDrl() {
            // Given
            String badDrl = "this is not valid DRL !!!";
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.empty());
            when(compilationService.compileDrl(anyString(), eq(1), eq(badDrl)))
                    .thenThrow(new DroolsCompilationService.CompilationException(
                            "DRL compilation failed", List.of("[ERROR] syntax error near '!!!'")
                    ));

            // When / Then
            assertThatThrownBy(() -> sut.register(RULE_ID, badDrl))
                    .isInstanceOf(RegisterDrlUseCase.DrlCompileException.class)
                    .hasMessageContaining("DRL compilation failed");

            verify(ruleRegistry, never()).save(any());
            verifyNoInteractions(ruleEnginePort);
        }
    }

    // ------------------------------------------------------------------
    // update()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Should compile new DRL, warm up bundle, update registry and return new bundleHash")
        void shouldUpdateExistingRule() {
            // Given
            String newDrl = "package rules; rule \"updated\" when eval(true) then end";
            String newHash = "sha256:newHash999";

            RegisteredRule existing = new RegisteredRule(
                    RULE_ID, VALID_DRL, BUNDLE_HASH, Instant.parse("2024-01-01T00:00:00Z"), Instant.now());
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.of(existing));

            DroolsCompilationService.CompilationResult compiled =
                    new DroolsCompilationService.CompilationResult(
                            newHash, new byte[]{9, 8, 7}, 3L, List.of(), "10.1.0");
            when(compilationService.compileDrl(eq(RULE_ID), eq(1), eq(newDrl))).thenReturn(compiled);

            // When
            RegisterDrlUseCase.RegisterRuleResult result = sut.update(RULE_ID, newDrl);

            // Then
            assertThat(result.ruleId()).isEqualTo(RULE_ID);
            assertThat(result.bundleHash()).isEqualTo(newHash);

            verify(ruleEnginePort).warmupBundle(eq(newHash), any(byte[].class));
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

            verifyNoInteractions(compilationService, ruleEnginePort);
        }

        @Test
        @DisplayName("Should throw DrlCompileException when new DRL is invalid")
        void shouldThrowCompileExceptionForInvalidNewDrl() {
            // Given
            String badDrl = "not valid drl";
            RegisteredRule existing = new RegisteredRule(
                    RULE_ID, VALID_DRL, BUNDLE_HASH, Instant.now(), Instant.now());
            when(ruleRegistry.findById(RULE_ID)).thenReturn(Optional.of(existing));
            when(compilationService.compileDrl(anyString(), eq(1), eq(badDrl)))
                    .thenThrow(new DroolsCompilationService.CompilationException(
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
