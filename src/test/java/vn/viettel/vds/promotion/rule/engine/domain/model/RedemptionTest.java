package vn.viettel.vds.promotion.rule.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Redemption Domain Model Tests")
class RedemptionTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create with default constructor and initialize metadata")
        void shouldCreateWithDefaultConstructor() {
            // When
            var redemption = new Redemption();

            // Then
            assertThat(redemption.getMetadata()).isNotNull().isEmpty();
            assertThat(redemption.isRedeemingCodeHolder()).isFalse();
        }

        @Test
        @DisplayName("Should create with parameters")
        void shouldCreateWithParameters() {
            // When
            var redemption = new Redemption(true, "LOC-001");

            // Then
            assertThat(redemption.isRedeemingCodeHolder()).isTrue();
            assertThat(redemption.getLocationId()).isEqualTo("LOC-001");
            assertThat(redemption.getMetadata()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("getMetadataValue()")
    class GetMetadataValueTests {

        @Test
        @DisplayName("Should return value when key exists")
        void shouldReturnValue_whenKeyExists() {
            // Given
            var redemption = new Redemption();
            redemption.setMetadata(Map.of("channel", "ONLINE", "source", "APP"));

            // When
            var result = redemption.getMetadataValue("channel");

            // Then
            assertThat(result).isEqualTo("ONLINE");
        }

        @Test
        @DisplayName("Should return null when key does not exist")
        void shouldReturnNull_whenKeyDoesNotExist() {
            // Given
            var redemption = new Redemption();

            // When
            var result = redemption.getMetadataValue("nonexistent");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null when metadata is null")
        void shouldReturnNull_whenMetadataIsNull() {
            // Given
            var redemption = new Redemption();
            // Force metadata to null via reflection would be needed, but we can test setMetadata(null) behavior

            // When - setMetadata(null) defaults to empty HashMap
            redemption.setMetadata(null);
            var result = redemption.getMetadataValue("key");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("setMetadata()")
    class SetMetadataTests {

        @Test
        @DisplayName("Should default to empty map when setting null")
        void shouldDefaultToEmptyMap_whenSettingNull() {
            // Given
            var redemption = new Redemption();

            // When
            redemption.setMetadata(null);

            // Then
            assertThat(redemption.getMetadata()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should set metadata map")
        void shouldSetMetadataMap() {
            // Given
            var redemption = new Redemption();
            var metadata = new HashMap<String, Object>();
            metadata.put("key1", "value1");

            // When
            redemption.setMetadata(metadata);

            // Then
            assertThat(redemption.getMetadata()).containsEntry("key1", "value1");
        }
    }

    @Nested
    @DisplayName("Properties")
    class PropertiesTests {

        @Test
        @DisplayName("Should set and get all properties")
        void shouldSetAndGetAllProperties() {
            // Given
            var redemption = new Redemption();

            // When
            redemption.setCodeHolder(true);
            redemption.setPerCustomerPerDay(3);
            redemption.setPerCustomerTotal(10);
            redemption.setRedeemingCodeHolder(false);
            redemption.setLocationId("LOC-002");
            redemption.setUserId("user-123");
            redemption.setApiKey("api-key-abc");

            // Then
            assertThat(redemption.isCodeHolder()).isTrue();
            assertThat(redemption.getPerCustomerPerDay()).isEqualTo(3);
            assertThat(redemption.getPerCustomerTotal()).isEqualTo(10);
            assertThat(redemption.isRedeemingCodeHolder()).isFalse();
            assertThat(redemption.getLocationId()).isEqualTo("LOC-002");
            assertThat(redemption.getUserId()).isEqualTo("user-123");
            assertThat(redemption.getApiKey()).isEqualTo("api-key-abc");
        }
    }
}
