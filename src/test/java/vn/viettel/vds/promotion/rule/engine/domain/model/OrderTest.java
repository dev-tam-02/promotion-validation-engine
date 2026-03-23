package vn.viettel.vds.promotion.rule.engine.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.rule.engine.TestFixtures;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order Domain Model Tests")
class OrderTest {

    private Order sut;

    @BeforeEach
    void setUp() {
        sut = TestFixtures.order("ord-1", BigDecimal.valueOf(50000));
    }

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("Should create order with id constructor")
        void shouldCreateOrderWithIdConstructor() {
            // When
            var order = new Order("ord-123");

            // Then
            assertThat(order.getId()).isEqualTo("ord-123");
            assertThat(order.getItems()).isEmpty();
        }

        @Test
        @DisplayName("Should create order with default constructor")
        void shouldCreateOrderWithDefaultConstructor() {
            // When
            var order = new Order();

            // Then
            assertThat(order.getId()).isNull();
            assertThat(order.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addItem()")
    class AddItemTests {

        @Test
        @DisplayName("Should add item to items list")
        void shouldAddItemToList() {
            // Given
            var item = TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1);

            // When
            sut.addItem(item);

            // Then
            assertThat(sut.getItems()).hasSize(1);
            assertThat(sut.getItems().get(0).getCategory()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("Should not add null item")
        void shouldNotAddNullItem() {
            // When
            sut.addItem(null);

            // Then
            assertThat(sut.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("setItems()")
    class SetItemsTests {

        @Test
        @DisplayName("Should set null items to empty list")
        void shouldSetNullItemsToEmptyList() {
            // When
            sut.setItems(null);

            // Then
            assertThat(sut.getItems()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should set items list")
        void shouldSetItemsList() {
            // Given
            var items = List.of(TestFixtures.orderItem("Food", "Brand", 50.0, 2));

            // When
            sut.setItems(items);

            // Then
            assertThat(sut.getItems()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("hasAnyItemInCategory()")
    class HasAnyItemInCategoryTests {

        @Test
        @DisplayName("Should return true when item in category exists")
        void shouldReturnTrue_whenItemInCategoryExists() {
            // Given
            sut.setItems(List.of(
                    TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1),
                    TestFixtures.orderItem("Food", "Nestle", 50.0, 2)
            ));

            // When & Then
            assertThat(sut.hasAnyItemInCategory("Electronics")).isTrue();
        }

        @Test
        @DisplayName("Should return false when no item in category")
        void shouldReturnFalse_whenNoItemInCategory() {
            // Given
            sut.setItems(List.of(TestFixtures.orderItem("Food", "Nestle", 50.0, 2)));

            // When & Then
            assertThat(sut.hasAnyItemInCategory("Electronics")).isFalse();
        }

        @Test
        @DisplayName("Should return false when category is null")
        void shouldReturnFalse_whenCategoryIsNull() {
            // Given
            sut.setItems(List.of(TestFixtures.orderItem("Food", "Nestle", 50.0, 2)));

            // When & Then
            assertThat(sut.hasAnyItemInCategory(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("hasEveryItemInCategory()")
    class HasEveryItemInCategoryTests {

        @Test
        @DisplayName("Should return true when all items in same category")
        void shouldReturnTrue_whenAllItemsInSameCategory() {
            // Given
            sut.setItems(List.of(
                    TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1),
                    TestFixtures.orderItem("Electronics", "Sony", 2000.0, 1)
            ));

            // When & Then
            assertThat(sut.hasEveryItemInCategory("Electronics")).isTrue();
        }

        @Test
        @DisplayName("Should return false when items in different categories")
        void shouldReturnFalse_whenItemsInDifferentCategories() {
            // Given
            sut.setItems(List.of(
                    TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1),
                    TestFixtures.orderItem("Food", "Nestle", 50.0, 2)
            ));

            // When & Then
            assertThat(sut.hasEveryItemInCategory("Electronics")).isFalse();
        }

        @Test
        @DisplayName("Should return false when items list is empty")
        void shouldReturnFalse_whenItemsEmpty() {
            // When & Then
            assertThat(sut.hasEveryItemInCategory("Electronics")).isFalse();
        }

        @Test
        @DisplayName("Should return false when category is null")
        void shouldReturnFalse_whenCategoryIsNull() {
            // Given
            sut.setItems(List.of(TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1)));

            // When & Then
            assertThat(sut.hasEveryItemInCategory(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("hasNoItemInCategory()")
    class HasNoItemInCategoryTests {

        @Test
        @DisplayName("Should return true when no item in category")
        void shouldReturnTrue_whenNoItemInCategory() {
            // Given
            sut.setItems(List.of(TestFixtures.orderItem("Food", "Nestle", 50.0, 2)));

            // When & Then
            assertThat(sut.hasNoItemInCategory("Electronics")).isTrue();
        }

        @Test
        @DisplayName("Should return false when item in category exists")
        void shouldReturnFalse_whenItemInCategoryExists() {
            // Given
            sut.setItems(List.of(TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1)));

            // When & Then
            assertThat(sut.hasNoItemInCategory("Electronics")).isFalse();
        }

        @Test
        @DisplayName("Should return true when category is null")
        void shouldReturnTrue_whenCategoryIsNull() {
            // When & Then
            assertThat(sut.hasNoItemInCategory(null)).isTrue();
        }
    }

    @Nested
    @DisplayName("hasAnyItemInBrand()")
    class HasAnyItemInBrandTests {

        @Test
        @DisplayName("Should return true when item with brand exists")
        void shouldReturnTrue_whenItemWithBrandExists() {
            // Given
            sut.setItems(List.of(TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1)));

            // When & Then
            assertThat(sut.hasAnyItemInBrand("Samsung")).isTrue();
        }

        @Test
        @DisplayName("Should return false when no item with brand")
        void shouldReturnFalse_whenNoItemWithBrand() {
            // Given
            sut.setItems(List.of(TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1)));

            // When & Then
            assertThat(sut.hasAnyItemInBrand("Sony")).isFalse();
        }

        @Test
        @DisplayName("Should return false when brand is null")
        void shouldReturnFalse_whenBrandIsNull() {
            // When & Then
            assertThat(sut.hasAnyItemInBrand(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("hasEveryItemInBrand()")
    class HasEveryItemInBrandTests {

        @Test
        @DisplayName("Should return true when all items have same brand")
        void shouldReturnTrue_whenAllItemsSameBrand() {
            // Given
            sut.setItems(List.of(
                    TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1),
                    TestFixtures.orderItem("Accessories", "Samsung", 200.0, 1)
            ));

            // When & Then
            assertThat(sut.hasEveryItemInBrand("Samsung")).isTrue();
        }

        @Test
        @DisplayName("Should return false when empty items")
        void shouldReturnFalse_whenEmptyItems() {
            // When & Then
            assertThat(sut.hasEveryItemInBrand("Samsung")).isFalse();
        }

        @Test
        @DisplayName("Should return false when brand is null")
        void shouldReturnFalse_whenBrandIsNull() {
            // Given
            sut.setItems(List.of(TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1)));

            // When & Then
            assertThat(sut.hasEveryItemInBrand(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("hasAnyItemInCollection()")
    class HasAnyItemInCollectionTests {

        @Test
        @DisplayName("Should return true when item with collection exists")
        void shouldReturnTrue_whenItemWithCollectionExists() {
            // Given
            OrderItem item = TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1);
            item.setCollection("Summer2026");
            sut.setItems(List.of(item));

            // When & Then
            assertThat(sut.hasAnyItemInCollection("Summer2026")).isTrue();
        }

        @Test
        @DisplayName("Should return false when collection is null")
        void shouldReturnFalse_whenCollectionIsNull() {
            // When & Then
            assertThat(sut.hasAnyItemInCollection(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("hasEveryItemInCollection()")
    class HasEveryItemInCollectionTests {

        @Test
        @DisplayName("Should return false when empty items")
        void shouldReturnFalse_whenEmptyItems() {
            // When & Then
            assertThat(sut.hasEveryItemInCollection("Summer2026")).isFalse();
        }

        @Test
        @DisplayName("Should return false when collection is null")
        void shouldReturnFalse_whenCollectionIsNull() {
            // Given
            OrderItem item = TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1);
            item.setCollection("Summer2026");
            sut.setItems(List.of(item));

            // When & Then
            assertThat(sut.hasEveryItemInCollection(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("getMostExpensiveItem()")
    class GetMostExpensiveItemTests {

        @Test
        @DisplayName("Should return most expensive item")
        void shouldReturnMostExpensiveItem() {
            // Given
            sut.setItems(List.of(
                    TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1),
                    TestFixtures.orderItem("Electronics", "Sony", 5000.0, 1),
                    TestFixtures.orderItem("Food", "Nestle", 50.0, 2)
            ));

            // When
            var result = sut.getMostExpensiveItem();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPrice()).isEqualTo(5000.0);
            assertThat(result.getBrand()).isEqualTo("Sony");
        }

        @Test
        @DisplayName("Should return null when items empty")
        void shouldReturnNull_whenItemsEmpty() {
            // When
            var result = sut.getMostExpensiveItem();

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getCheapestItem()")
    class GetCheapestItemTests {

        @Test
        @DisplayName("Should return cheapest item")
        void shouldReturnCheapestItem() {
            // Given
            sut.setItems(List.of(
                    TestFixtures.orderItem("Electronics", "Samsung", 1000.0, 1),
                    TestFixtures.orderItem("Food", "Nestle", 50.0, 2)
            ));

            // When
            var result = sut.getCheapestItem();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPrice()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return null when items empty")
        void shouldReturnNull_whenItemsEmpty() {
            // When
            var result = sut.getCheapestItem();

            // Then
            assertThat(result).isNull();
        }
    }
}
