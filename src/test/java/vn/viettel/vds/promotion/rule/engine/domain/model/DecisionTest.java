package vn.viettel.vds.promotion.rule.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Decision Domain Model Tests")
class DecisionTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create valid decision")
        void shouldCreateValidDecision() {
            // Given
            var candidate = new Candidate("voucher", "PROMO10", "v-001");

            // When
            var decision = new Decision(candidate, true);

            // Then
            assertThat(decision.getCandidate()).isNotNull();
            assertThat(decision.getCandidate().getCode()).isEqualTo("PROMO10");
            assertThat(decision.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should create invalid decision")
        void shouldCreateInvalidDecision() {
            // Given
            var candidate = new Candidate("voucher", "EXPIRED");

            // When
            var decision = new Decision(candidate, false);

            // Then
            assertThat(decision.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should create decision with default constructor")
        void shouldCreateWithDefaultConstructor() {
            // When
            var decision = new Decision();

            // Then
            assertThat(decision.getCandidate()).isNull();
            assertThat(decision.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Properties")
    class PropertiesTests {

        @Test
        @DisplayName("Should set and get reasons")
        void shouldSetAndGetReasons() {
            // Given
            var decision = new Decision();
            var reasons = List.of(new ReasonCode("BUDGET_EXCEEDED"), new ReasonCode("EXPIRED"));

            // When
            decision.setReasons(reasons);

            // Then
            assertThat(decision.getReasons()).hasSize(2);
            assertThat(decision.getReasons().get(0).getCode()).isEqualTo("BUDGET_EXCEEDED");
        }

        @Test
        @DisplayName("Should set and get limits remaining")
        void shouldSetAndGetLimitsRemaining() {
            // Given
            var decision = new Decision();

            // When
            decision.setLimitsRemaining(Map.of("perDay", 5, "perCustomer", 2));

            // Then
            assertThat(decision.getLimitsRemaining())
                    .containsEntry("perDay", 5)
                    .containsEntry("perCustomer", 2);
        }

        @Test
        @DisplayName("Should set and get decision token")
        void shouldSetAndGetDecisionToken() {
            // Given
            var decision = new Decision();

            // When
            decision.setDecisionToken("token-abc-123");

            // Then
            assertThat(decision.getDecisionToken()).isEqualTo("token-abc-123");
        }
    }
}
