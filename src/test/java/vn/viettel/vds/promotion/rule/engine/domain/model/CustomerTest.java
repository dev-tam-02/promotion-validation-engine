package vn.viettel.vds.promotion.rule.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Customer Domain Model Tests")
class CustomerTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create customer with id and segments")
        void shouldCreateWithIdAndSegments() {
            // When
            var customer = new Customer("cust-1", Set.of("VIP", "PREMIUM"));

            // Then
            assertThat(customer.getId()).isEqualTo("cust-1");
            assertThat(customer.getSegments()).containsExactlyInAnyOrder("VIP", "PREMIUM");
        }

        @Test
        @DisplayName("Should create customer with default constructor")
        void shouldCreateWithDefaultConstructor() {
            // When
            var customer = new Customer();

            // Then
            assertThat(customer.getId()).isNull();
            assertThat(customer.getSegments()).isNull();
        }
    }

    @Nested
    @DisplayName("Tier alias methods")
    class TierAliasTests {

        @Test
        @DisplayName("Should getTier return loyaltyTier value")
        void shouldGetTierReturnLoyaltyTier() {
            // Given
            var customer = new Customer();
            customer.setLoyaltyTier("GOLD");

            // Then
            assertThat(customer.getTier()).isEqualTo("GOLD");
        }

        @Test
        @DisplayName("Should setTier set loyaltyTier value")
        void shouldSetTierSetLoyaltyTier() {
            // Given
            var customer = new Customer();

            // When
            customer.setTier("PLATINUM");

            // Then
            assertThat(customer.getLoyaltyTier()).isEqualTo("PLATINUM");
        }

        @Test
        @DisplayName("Should getTier and getLoyaltyTier be consistent")
        void shouldTierAndLoyaltyTierBeConsistent() {
            // Given
            var customer = new Customer();
            customer.setTier("SILVER");

            // Then
            assertThat(customer.getTier()).isEqualTo(customer.getLoyaltyTier());
        }
    }

    @Nested
    @DisplayName("Properties")
    class PropertiesTests {

        @Test
        @DisplayName("Should set and get all properties")
        void shouldSetAndGetAllProperties() {
            // Given
            var customer = new Customer();

            // When
            customer.setId("cust-123");
            customer.setLoyaltyTier("GOLD");
            customer.setLoyaltyPoints(5000);
            customer.setLifetimeValue(150000.0);
            customer.setTotalOrdersValue(80000.0);
            customer.setTotalDiscountedAmount(10000.0);
            customer.setRedemptionsPerDay(3);
            customer.setRedemptionsPerIncentive(1);
            customer.setRedeemingCodeHolder(true);
            customer.setAcquisitionChannel("ONLINE");
            customer.setAttrs(Map.of("region", "HCM"));

            // Then
            assertThat(customer.getId()).isEqualTo("cust-123");
            assertThat(customer.getLoyaltyTier()).isEqualTo("GOLD");
            assertThat(customer.getLoyaltyPoints()).isEqualTo(5000);
            assertThat(customer.getLifetimeValue()).isEqualTo(150000.0);
            assertThat(customer.getTotalOrdersValue()).isEqualTo(80000.0);
            assertThat(customer.getTotalDiscountedAmount()).isEqualTo(10000.0);
            assertThat(customer.getRedemptionsPerDay()).isEqualTo(3);
            assertThat(customer.getRedemptionsPerIncentive()).isEqualTo(1);
            assertThat(customer.isRedeemingCodeHolder()).isTrue();
            assertThat(customer.getAcquisitionChannel()).isEqualTo("ONLINE");
            assertThat(customer.getAttrs()).containsEntry("region", "HCM");
        }
    }
}
