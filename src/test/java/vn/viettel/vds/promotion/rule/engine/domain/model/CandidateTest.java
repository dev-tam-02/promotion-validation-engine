package vn.viettel.vds.promotion.rule.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Candidate Domain Model Tests")
class CandidateTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create candidate with type and code")
        void shouldCreateWithTypeAndCode() {
            // When
            var candidate = new Candidate("voucher", "PROMO10");

            // Then
            assertThat(candidate.getType()).isEqualTo("voucher");
            assertThat(candidate.getCode()).isEqualTo("PROMO10");
            assertThat(candidate.getId()).isNull();
        }

        @Test
        @DisplayName("Should create candidate with type, code, and id")
        void shouldCreateWithTypeCodeAndId() {
            // When
            var candidate = new Candidate("tier", "GOLD", "tier-001");

            // Then
            assertThat(candidate.getType()).isEqualTo("tier");
            assertThat(candidate.getCode()).isEqualTo("GOLD");
            assertThat(candidate.getId()).isEqualTo("tier-001");
        }

        @Test
        @DisplayName("Should create candidate with default constructor")
        void shouldCreateWithDefaultConstructor() {
            // When
            var candidate = new Candidate();

            // Then
            assertThat(candidate.getType()).isNull();
            assertThat(candidate.getCode()).isNull();
            assertThat(candidate.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("isVoucher()")
    class IsVoucherTests {

        @Test
        @DisplayName("Should return true when type is voucher")
        void shouldReturnTrue_whenTypeIsVoucher() {
            // Given
            var candidate = new Candidate("voucher", "CODE");

            // Then
            assertThat(candidate.isVoucher()).isTrue();
        }

        @Test
        @DisplayName("Should return false when type is not voucher")
        void shouldReturnFalse_whenTypeIsNotVoucher() {
            // Given
            var candidate = new Candidate("tier", "GOLD");

            // Then
            assertThat(candidate.isVoucher()).isFalse();
        }

        @Test
        @DisplayName("Should return false when type is null")
        void shouldReturnFalse_whenTypeIsNull() {
            // Given
            var candidate = new Candidate();

            // Then
            assertThat(candidate.isVoucher()).isFalse();
        }
    }

    @Nested
    @DisplayName("isTier()")
    class IsTierTests {

        @Test
        @DisplayName("Should return true when type is tier")
        void shouldReturnTrue_whenTypeIsTier() {
            // Given
            var candidate = new Candidate("tier", "GOLD");

            // Then
            assertThat(candidate.isTier()).isTrue();
        }

        @Test
        @DisplayName("Should return false when type is not tier")
        void shouldReturnFalse_whenTypeIsNotTier() {
            // Given
            var candidate = new Candidate("voucher", "CODE");

            // Then
            assertThat(candidate.isTier()).isFalse();
        }
    }

    @Nested
    @DisplayName("isLoyalty()")
    class IsLoyaltyTests {

        @Test
        @DisplayName("Should return true when type is loyalty")
        void shouldReturnTrue_whenTypeIsLoyalty() {
            // Given
            var candidate = new Candidate("loyalty", "POINTS");

            // Then
            assertThat(candidate.isLoyalty()).isTrue();
        }

        @Test
        @DisplayName("Should return false when type is not loyalty")
        void shouldReturnFalse_whenTypeIsNotLoyalty() {
            // Given
            var candidate = new Candidate("voucher", "CODE");

            // Then
            assertThat(candidate.isLoyalty()).isFalse();
        }
    }
}
