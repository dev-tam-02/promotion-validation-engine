package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NewOperatorTranslatorsTest {

    @Test
    void testOrderItemBrandInOperator() {
        OrderItemBrandInOperatorTranslator translator = new OrderItemBrandInOperatorTranslator();
        
        Map<String, Object> params = Map.of("brands", List.of("Nike", "Adidas"));
        String result = translator.translate("node1", params, "BRAND_NOT_MATCH");
        
        assertTrue(result.contains("brand in (\"Nike\", \"Adidas\")"));
        assertEquals("order.item.brand.in", translator.getOperatorName());
    }

    @Test
    void testOrderItemCollectionInOperator() {
        OrderItemCollectionInOperatorTranslator translator = new OrderItemCollectionInOperatorTranslator();
        
        Map<String, Object> params = Map.of("collections", List.of("Summer2024", "NewArrivals"));
        String result = translator.translate("node1", params, "COLLECTION_NOT_MATCH");
        
        assertTrue(result.contains("collection in (\"Summer2024\", \"NewArrivals\")"));
        assertEquals("order.item.collection.in", translator.getOperatorName());
    }

    @Test
    void testOrderItemPriceGteOperator() {
        OrderItemPriceGteOperatorTranslator translator = new OrderItemPriceGteOperatorTranslator();
        
        Map<String, Object> params = Map.of("minPrice", 100000);
        String result = translator.translate("node1", params, "PRICE_TOO_LOW");
        
        assertTrue(result.contains("price >= 100000"));
        assertEquals("order.item.price.gte", translator.getOperatorName());
    }

    @Test
    void testOrderItemPriceBetweenOperator() {
        OrderItemPriceBetweenOperatorTranslator translator = new OrderItemPriceBetweenOperatorTranslator();
        
        Map<String, Object> params = Map.of("minPrice", 50000, "maxPrice", 200000);
        String result = translator.translate("node1", params, "PRICE_OUT_OF_RANGE");
        
        assertTrue(result.contains("price >= 50000 && price <= 200000"));
        assertEquals("order.item.price.between", translator.getOperatorName());
    }

    @Test
    void testCustomerLifetimeValueGteOperator() {
        CustomerLifetimeValueGteOperatorTranslator translator = new CustomerLifetimeValueGteOperatorTranslator();
        
        Map<String, Object> params = Map.of("minValue", 1000000);
        String result = translator.translate("node1", params, "LIFETIME_VALUE_TOO_LOW");
        
        assertTrue(result.contains("lifetimeValue >= 1000000"));
        assertEquals("customer.lifetime.value.gte", translator.getOperatorName());
    }

    @Test
    void testCustomerMetadataEqualsOperator() {
        CustomerMetadataEqualsOperatorTranslator translator = new CustomerMetadataEqualsOperatorTranslator();
        
        Map<String, Object> params = Map.of("key", "region", "value", "HCM");
        String result = translator.translate("node1", params, "METADATA_NOT_MATCH");
        
        assertTrue(result.contains("attrs[\"region\"] == \"HCM\""));
        assertEquals("customer.metadata.equals", translator.getOperatorName());
    }

    @Test
    void testOrderMetadataEqualsOperator() {
        OrderMetadataEqualsOperatorTranslator translator = new OrderMetadataEqualsOperatorTranslator();
        
        Map<String, Object> params = Map.of("key", "shipping_method", "value", "EXPRESS");
        String result = translator.translate("node1", params, "SHIPPING_NOT_MATCH");
        
        assertTrue(result.contains("metadata[\"shipping_method\"] == \"EXPRESS\""));
        assertEquals("order.metadata.equals", translator.getOperatorName());
    }
}
