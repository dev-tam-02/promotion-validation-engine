package vn.viettel.vds.promotion.rule.engine.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LimitsCtx Domain Model Tests")
class LimitsCtxTest {

    private LimitsCtx sut;

    @BeforeEach
    void setUp() {
        sut = new LimitsCtx();
    }

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should initialize monetary fields to zero")
        void shouldInitializeMonetaryFieldsToZero() {
            // Then
            assertThat(sut.getTotalDiscountedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(sut.getTotalOrdersValue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(sut.getTotalGiftAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(sut.getTotalPayWithPoints()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should create with parameters")
        void shouldCreateWithParameters() {
            // When
            var ctx = new LimitsCtx(100, 5, java.time.LocalDate.of(2026, 3, 21));

            // Then
            assertThat(ctx.getPerCodeTotalUsed()).isEqualTo(100);
            assertThat(ctx.getPerCustomerUsed()).isEqualTo(5);
            assertThat(ctx.getDate()).isEqualTo(java.time.LocalDate.of(2026, 3, 21));
            assertThat(ctx.getTotalDiscountedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("isWithinTotalRedemptionsLimit()")
    class TotalRedemptionsLimitTests {

        @Test
        @DisplayName("Should return true when max is zero (unlimited)")
        void shouldReturnTrue_whenMaxIsZero() {
            // Given
            sut.setTotalRedemptions(100);
            sut.setMaxTotalRedemptions(0);

            // Then
            assertThat(sut.isWithinTotalRedemptionsLimit()).isTrue();
        }

        @Test
        @DisplayName("Should return true when below limit")
        void shouldReturnTrue_whenBelowLimit() {
            // Given
            sut.setTotalRedemptions(5);
            sut.setMaxTotalRedemptions(10);

            // Then
            assertThat(sut.isWithinTotalRedemptionsLimit()).isTrue();
        }

        @Test
        @DisplayName("Should return false when at limit")
        void shouldReturnFalse_whenAtLimit() {
            // Given
            sut.setTotalRedemptions(10);
            sut.setMaxTotalRedemptions(10);

            // Then
            assertThat(sut.isWithinTotalRedemptionsLimit()).isFalse();
        }

        @Test
        @DisplayName("Should return false when above limit")
        void shouldReturnFalse_whenAboveLimit() {
            // Given
            sut.setTotalRedemptions(15);
            sut.setMaxTotalRedemptions(10);

            // Then
            assertThat(sut.isWithinTotalRedemptionsLimit()).isFalse();
        }
    }

    @Nested
    @DisplayName("isWithinDailyRedemptionsLimit()")
    class DailyRedemptionsLimitTests {

        @Test
        @DisplayName("Should return true when max is zero (unlimited)")
        void shouldReturnTrue_whenUnlimited() {
            // Given
            sut.setRedemptionsPerDay(100);
            sut.setMaxRedemptionsPerDay(0);

            // Then
            assertThat(sut.isWithinDailyRedemptionsLimit()).isTrue();
        }

        @Test
        @DisplayName("Should return true when below limit")
        void shouldReturnTrue_whenBelowLimit() {
            // Given
            sut.setRedemptionsPerDay(3);
            sut.setMaxRedemptionsPerDay(5);

            // Then
            assertThat(sut.isWithinDailyRedemptionsLimit()).isTrue();
        }

        @Test
        @DisplayName("Should return false when at or above limit")
        void shouldReturnFalse_whenAtLimit() {
            // Given
            sut.setRedemptionsPerDay(5);
            sut.setMaxRedemptionsPerDay(5);

            // Then
            assertThat(sut.isWithinDailyRedemptionsLimit()).isFalse();
        }
    }

    @Nested
    @DisplayName("isWithinMonthlyRedemptionsLimit()")
    class MonthlyRedemptionsLimitTests {

        @Test
        @DisplayName("Should return true when below limit")
        void shouldReturnTrue_whenBelowLimit() {
            // Given
            sut.setRedemptionsPerMonth(10);
            sut.setMaxRedemptionsPerMonth(100);

            // Then
            assertThat(sut.isWithinMonthlyRedemptionsLimit()).isTrue();
        }

        @Test
        @DisplayName("Should return true when unlimited")
        void shouldReturnTrue_whenUnlimited() {
            // Given
            sut.setRedemptionsPerMonth(999);
            sut.setMaxRedemptionsPerMonth(0);

            // Then
            assertThat(sut.isWithinMonthlyRedemptionsLimit()).isTrue();
        }
    }

    @Nested
    @DisplayName("isWithinPerCustomerLimit()")
    class PerCustomerLimitTests {

        @Test
        @DisplayName("Should return true when below limit")
        void shouldReturnTrue_whenBelowLimit() {
            // Given
            sut.setPerCustomerUsed(2);
            sut.setMaxPerCustomer(5);

            // Then
            assertThat(sut.isWithinPerCustomerLimit()).isTrue();
        }

        @Test
        @DisplayName("Should return false when at limit")
        void shouldReturnFalse_whenAtLimit() {
            // Given
            sut.setPerCustomerUsed(5);
            sut.setMaxPerCustomer(5);

            // Then
            assertThat(sut.isWithinPerCustomerLimit()).isFalse();
        }
    }

    @Nested
    @DisplayName("isWithinPerCustomerPerDayLimit()")
    class PerCustomerPerDayLimitTests {

        @Test
        @DisplayName("Should return true when below limit")
        void shouldReturnTrue_whenBelowLimit() {
            // Given
            sut.setPerCustomerPerDay(1);
            sut.setMaxPerCustomerPerDay(3);

            // Then
            assertThat(sut.isWithinPerCustomerPerDayLimit()).isTrue();
        }
    }

    @Nested
    @DisplayName("isWithinPerCustomerPerMonthLimit()")
    class PerCustomerPerMonthLimitTests {

        @Test
        @DisplayName("Should return true when below limit")
        void shouldReturnTrue_whenBelowLimit() {
            // Given
            sut.setPerCustomerPerMonth(5);
            sut.setMaxPerCustomerPerMonth(10);

            // Then
            assertThat(sut.isWithinPerCustomerPerMonthLimit()).isTrue();
        }
    }

    @Nested
    @DisplayName("isWithinDiscountedAmountLimit()")
    class DiscountedAmountLimitTests {

        @Test
        @DisplayName("Should return true when max is null (unlimited)")
        void shouldReturnTrue_whenMaxIsNull() {
            // Given
            sut.setTotalDiscountedAmount(BigDecimal.valueOf(999999));
            sut.setMaxDiscountedAmount(null);

            // Then
            assertThat(sut.isWithinDiscountedAmountLimit()).isTrue();
        }

        @Test
        @DisplayName("Should return true when below limit")
        void shouldReturnTrue_whenBelowLimit() {
            // Given
            sut.setTotalDiscountedAmount(BigDecimal.valueOf(500));
            sut.setMaxDiscountedAmount(BigDecimal.valueOf(1000));

            // Then
            assertThat(sut.isWithinDiscountedAmountLimit()).isTrue();
        }

        @Test
        @DisplayName("Should return false when at or above limit")
        void shouldReturnFalse_whenAtLimit() {
            // Given
            sut.setTotalDiscountedAmount(BigDecimal.valueOf(1000));
            sut.setMaxDiscountedAmount(BigDecimal.valueOf(1000));

            // Then
            assertThat(sut.isWithinDiscountedAmountLimit()).isFalse();
        }
    }

    @Nested
    @DisplayName("isWithinOrdersValueLimit()")
    class OrdersValueLimitTests {

        @Test
        @DisplayName("Should return true when max is null")
        void shouldReturnTrue_whenMaxIsNull() {
            // Given
            sut.setMaxOrdersValue(null);

            // Then
            assertThat(sut.isWithinOrdersValueLimit()).isTrue();
        }
    }

    @Nested
    @DisplayName("isWithinGiftAmountLimit()")
    class GiftAmountLimitTests {

        @Test
        @DisplayName("Should return true when max is null")
        void shouldReturnTrue_whenMaxIsNull() {
            // Given
            sut.setMaxGiftAmount(null);

            // Then
            assertThat(sut.isWithinGiftAmountLimit()).isTrue();
        }
    }

    @Nested
    @DisplayName("isWithinPayWithPointsLimit()")
    class PayWithPointsLimitTests {

        @Test
        @DisplayName("Should return true when max is null")
        void shouldReturnTrue_whenMaxIsNull() {
            // Given
            sut.setMaxPayWithPoints(null);

            // Then
            assertThat(sut.isWithinPayWithPointsLimit()).isTrue();
        }

        @Test
        @DisplayName("Should return true when below limit")
        void shouldReturnTrue_whenBelowLimit() {
            // Given
            sut.setTotalPayWithPoints(BigDecimal.valueOf(100));
            sut.setMaxPayWithPoints(BigDecimal.valueOf(500));

            // Then
            assertThat(sut.isWithinPayWithPointsLimit()).isTrue();
        }
    }

    @Nested
    @DisplayName("Null-safe setters for monetary fields")
    class NullSafeSettersTests {

        @Test
        @DisplayName("Should default to zero when setting null discounted amount")
        void shouldDefaultToZero_whenNullDiscountedAmount() {
            // When
            sut.setTotalDiscountedAmount(null);

            // Then
            assertThat(sut.getTotalDiscountedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should default to zero when setting null orders value")
        void shouldDefaultToZero_whenNullOrdersValue() {
            // When
            sut.setTotalOrdersValue(null);

            // Then
            assertThat(sut.getTotalOrdersValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should default to zero when setting null gift amount")
        void shouldDefaultToZero_whenNullGiftAmount() {
            // When
            sut.setTotalGiftAmount(null);

            // Then
            assertThat(sut.getTotalGiftAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should default to zero when setting null pay with points")
        void shouldDefaultToZero_whenNullPayWithPoints() {
            // When
            sut.setTotalPayWithPoints(null);

            // Then
            assertThat(sut.getTotalPayWithPoints()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
