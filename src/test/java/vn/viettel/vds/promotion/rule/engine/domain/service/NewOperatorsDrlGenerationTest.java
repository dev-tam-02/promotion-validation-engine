package vn.viettel.vds.promotion.rule.engine.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslatorRegistry;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewOperatorsDrlGenerationTest {

    @Mock
    private OperatorTranslatorRegistry translatorRegistry;

    private RuleTranslationService ruleTranslationService;

    @BeforeEach
    void setUp() {
        ruleTranslationService = new RuleTranslationService(translatorRegistry);
    }

    @Test
    void testBrandOperatorDrlGeneration() {
        // Given
        OrderItemBrandInOperatorTranslator brandTranslator = new OrderItemBrandInOperatorTranslator();
        when(translatorRegistry.findTranslator(eq("order.item.brand.in"), any()))
                .thenReturn(Optional.of(brandTranslator));

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "node1",
                        "type", "COND",
                        "operator", "order.item.brand.in",
                        "version", 1,
                        "params", Map.of("brands", List.of("Nike", "Adidas")),
                        "reasonCode", "BRAND_NOT_ALLOWED"
                )
        );

        // When
        String drl = ruleTranslationService.translateToDrl("tenant1", nodes);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("package tenant1"));
        assertTrue(drl.contains("brand in (\"Nike\", \"Adidas\")"));
        assertTrue(drl.contains("reasonCodes.add(\"BRAND_NOT_ALLOWED\")"));
        System.out.println("Brand DRL:\n" + drl);
    }

    @Test
    void testCollectionOperatorDrlGeneration() {
        // Given
        OrderItemCollectionInOperatorTranslator collectionTranslator = new OrderItemCollectionInOperatorTranslator();
        when(translatorRegistry.findTranslator(eq("order.item.collection.in"), any()))
                .thenReturn(Optional.of(collectionTranslator));

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "node1",
                        "type", "COND",
                        "operator", "order.item.collection.in",
                        "version", 1,
                        "params", Map.of("collections", List.of("Summer2024", "NewArrivals")),
                        "reasonCode", "COLLECTION_NOT_ALLOWED"
                )
        );

        // When
        String drl = ruleTranslationService.translateToDrl("tenant1", nodes);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("collection in (\"Summer2024\", \"NewArrivals\")"));
        assertTrue(drl.contains("reasonCodes.add(\"COLLECTION_NOT_ALLOWED\")"));
        System.out.println("Collection DRL:\n" + drl);
    }

    @Test
    void testPriceRangeOperatorDrlGeneration() {
        // Given
        OrderItemPriceBetweenOperatorTranslator priceTranslator = new OrderItemPriceBetweenOperatorTranslator();
        when(translatorRegistry.findTranslator(eq("order.item.price.between"), any()))
                .thenReturn(Optional.of(priceTranslator));

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "node1",
                        "type", "COND",
                        "operator", "order.item.price.between",
                        "version", 1,
                        "params", Map.of("minPrice", 100000, "maxPrice", 500000),
                        "reasonCode", "PRICE_OUT_OF_RANGE"
                )
        );

        // When
        String drl = ruleTranslationService.translateToDrl("tenant1", nodes);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("price >= 100000 && price <= 500000"));
        assertTrue(drl.contains("reasonCodes.add(\"PRICE_OUT_OF_RANGE\")"));
        System.out.println("Price Range DRL:\n" + drl);
    }

    @Test
    void testCustomerMetadataOperatorDrlGeneration() {
        // Given
        CustomerMetadataEqualsOperatorTranslator metadataTranslator = new CustomerMetadataEqualsOperatorTranslator();
        when(translatorRegistry.findTranslator(eq("customer.metadata.equals"), any()))
                .thenReturn(Optional.of(metadataTranslator));

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "node1",
                        "type", "COND",
                        "operator", "customer.metadata.equals",
                        "version", 1,
                        "params", Map.of("key", "region", "value", "HCM"),
                        "reasonCode", "REGION_NOT_ALLOWED"
                )
        );

        // When
        String drl = ruleTranslationService.translateToDrl("tenant1", nodes);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("attrs[\"region\"] == \"HCM\""));
        assertTrue(drl.contains("reasonCodes.add(\"REGION_NOT_ALLOWED\")"));
        System.out.println("Customer Metadata DRL:\n" + drl);
    }

    @Test
    void testComplexRuleWithMultipleNewOperators() {
        // Given - Setup all translators
        when(translatorRegistry.findTranslator(eq("order.item.brand.in"), any()))
                .thenReturn(Optional.of(new OrderItemBrandInOperatorTranslator()));
        when(translatorRegistry.findTranslator(eq("customer.lifetime.value.gte"), any()))
                .thenReturn(Optional.of(new CustomerLifetimeValueGteOperatorTranslator()));
        when(translatorRegistry.findTranslator(eq("order.metadata.equals"), any()))
                .thenReturn(Optional.of(new OrderMetadataEqualsOperatorTranslator()));

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "group1",
                        "type", "GROUP",
                        "operator", "AND",
                        "children", List.of("node1", "node2", "node3")
                ),
                Map.of(
                        "id", "node1",
                        "type", "COND",
                        "operator", "order.item.brand.in",
                        "version", 1,
                        "params", Map.of("brands", List.of("Nike", "Adidas")),
                        "reasonCode", "BRAND_NOT_ALLOWED"
                ),
                Map.of(
                        "id", "node2",
                        "type", "COND",
                        "operator", "customer.lifetime.value.gte",
                        "version", 1,
                        "params", Map.of("minValue", 1000000),
                        "reasonCode", "LIFETIME_VALUE_TOO_LOW"
                ),
                Map.of(
                        "id", "node3",
                        "type", "COND",
                        "operator", "order.metadata.equals",
                        "version", 1,
                        "params", Map.of("key", "channel", "value", "MOBILE"),
                        "reasonCode", "CHANNEL_NOT_ALLOWED"
                )
        );

        // When
        String drl = ruleTranslationService.translateToDrl("tenant1", nodes);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("brand in (\"Nike\", \"Adidas\")"));
        assertTrue(drl.contains("lifetimeValue >= 1000000"));
        assertTrue(drl.contains("metadata[\"channel\"] == \"MOBILE\""));
        assertTrue(drl.contains("reasonCodes.add(\"BRAND_NOT_ALLOWED\")"));
        assertTrue(drl.contains("reasonCodes.add(\"LIFETIME_VALUE_TOO_LOW\")"));
        assertTrue(drl.contains("reasonCodes.add(\"CHANNEL_NOT_ALLOWED\")"));
        
        System.out.println("Complex Rule DRL:\n" + drl);
    }
}
