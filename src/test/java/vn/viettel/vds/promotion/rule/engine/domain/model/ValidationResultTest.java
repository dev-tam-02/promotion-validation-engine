package vn.viettel.vds.promotion.rule.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationResult Domain Model Tests")
class ValidationResultTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create matched result")
        void shouldCreateMatchedResult() {
            // When
            var result = new ValidationResult(true, "All conditions matched");

            // Then
            assertThat(result.isMatched()).isTrue();
            assertThat(result.getMessage()).isEqualTo("All conditions matched");
        }

        @Test
        @DisplayName("Should create unmatched result")
        void shouldCreateUnmatchedResult() {
            // When
            var result = new ValidationResult(false, "Budget exceeded");

            // Then
            assertThat(result.isMatched()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Budget exceeded");
        }

        @Test
        @DisplayName("Should create result with default constructor")
        void shouldCreateWithDefaultConstructor() {
            // When
            var result = new ValidationResult();

            // Then
            assertThat(result.isMatched()).isFalse();
            assertThat(result.getMessage()).isNull();
            assertThat(result.getOk()).isNull();
        }
    }

    @Nested
    @DisplayName("Rule execution fields")
    class RuleExecutionFieldsTests {

        @Test
        @DisplayName("Should set and get ok, decision, and reason codes")
        void shouldSetAndGetRuleExecutionFields() {
            // Given
            var result = new ValidationResult();

            // When
            result.setOk(true);
            result.setDecision("ALLOW");
            result.setReasonCodes(List.of("ELIGIBLE", "BUDGET_OK"));
            result.setCandidateId("cand-001");

            // Then
            assertThat(result.getOk()).isTrue();
            assertThat(result.getDecision()).isEqualTo("ALLOW");
            assertThat(result.getReasonCodes()).containsExactly("ELIGIBLE", "BUDGET_OK");
            assertThat(result.getCandidateId()).isEqualTo("cand-001");
        }

        @Test
        @DisplayName("Should represent DENY decision")
        void shouldRepresentDenyDecision() {
            // Given
            var result = new ValidationResult(false, "Expired");

            // When
            result.setOk(false);
            result.setDecision("DENY");
            result.setReasonCodes(List.of("EXPIRED", "BUDGET_EXCEEDED"));

            // Then
            assertThat(result.getOk()).isFalse();
            assertThat(result.getDecision()).isEqualTo("DENY");
            assertThat(result.getReasonCodes()).hasSize(2);
        }
    }
}
