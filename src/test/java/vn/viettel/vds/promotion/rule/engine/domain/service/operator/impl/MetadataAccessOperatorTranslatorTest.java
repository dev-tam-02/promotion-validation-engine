package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetadataAccessOperatorTranslatorTest {

    private MetadataAccessOperatorTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new MetadataAccessOperatorTranslator();
    }

    // --- metadata ---

    @Test
    void getOperatorName() {
        assertThat(translator.getOperatorName()).isEqualTo("metadata.access");
    }

    @Test
    void supports_matchingName_true() {
        assertThat(translator.supports("metadata.access", null)).isTrue();
        assertThat(translator.supports("metadata.access", 1)).isTrue();
    }

    @Test
    void supports_differentName_false() {
        assertThat(translator.supports("customer.metadata.equals", null)).isFalse();
    }

    // --- STRING ---

    @Test
    void string_equals_customerMetadata() {
        String result = translate("customer", "vip_tier", "STRING", "equals", "platinum");
        assertThat(result).contains("$customer: Customer(");
        assertThat(result).contains("attrs != null");
        assertThat(result).contains("attrs[\"vip_tier\"] == \"platinum\"");
    }

    @Test
    void string_notEquals_customerMetadata() {
        String result = translate("customer_metadata", "vip_tier", "STRING", "not_equals", "basic");
        assertThat(result).contains("attrs[\"vip_tier\"] != \"basic\"");
    }

    @Test
    void string_in_multipleValues() {
        String result = translate("customer", "vip_tier", "STRING", "in", List.of("gold", "platinum"));
        assertThat(result).contains("attrs[\"vip_tier\"] in (\"gold\", \"platinum\")");
    }

    @Test
    void string_notIn_values() {
        String result = translate("customer", "vip_tier", "STRING", "not_in", List.of("basic"));
        assertThat(result).contains("attrs[\"vip_tier\"] not in (\"basic\")");
    }

    @Test
    void string_contains() {
        String result = translate("customer", "region", "STRING", "contains", "HCM");
        assertThat(result).contains("((String) attrs[\"region\"]).contains(\"HCM\")");
    }

    @Test
    void string_startsWith() {
        String result = translate("customer", "region", "STRING", "starts_with", "HN");
        assertThat(result).contains("((String) attrs[\"region\"]).startsWith(\"HN\")");
    }

    // --- NUMBER ---

    @Test
    void number_gte_customerMetadata() {
        String result = translate("customer", "loyalty_score", "NUMBER", "gte", 800);
        assertThat(result).contains("((Number) attrs[\"loyalty_score\"]).doubleValue() >= 800");
    }

    @Test
    void number_lte() {
        String result = translate("customer", "loyalty_score", "NUMBER", "lte", 1000);
        assertThat(result).contains(".doubleValue() <= 1000");
    }

    @Test
    void number_equals() {
        String result = translate("customer", "loyalty_score", "NUMBER", "equals", 500);
        assertThat(result).contains(".doubleValue() == 500");
    }

    @Test
    void number_between() {
        String result = translate("customer", "loyalty_score", "NUMBER", "between",
                Map.of("min", 100, "max", 500));
        assertThat(result).contains(".doubleValue() >= 100");
        assertThat(result).contains(".doubleValue() <= 500");
    }

    // --- BOOLEAN ---

    @Test
    void boolean_isTrue_orderMetadata() {
        String result = translate("order", "gift_wrap_requested", "BOOLEAN", "is_true", null);
        assertThat(result).contains("$order: Order(");
        assertThat(result).contains("metadata != null");
        assertThat(result).contains("Boolean.TRUE.equals(metadata[\"gift_wrap_requested\"])");
    }

    @Test
    void boolean_isFalse() {
        String result = translate("order_metadata", "express_required", "BOOLEAN", "is_false", null);
        assertThat(result).contains("Boolean.FALSE.equals(metadata[\"express_required\"])");
    }

    // --- DATE ---

    @Test
    void date_after_customerMetadata() {
        String result = translate("customer", "first_purchase_date", "DATE", "after", "2025-01-01T00:00:00Z");
        assertThat(result).contains("((java.time.Instant) attrs[\"first_purchase_date\"])");
        assertThat(result).contains(".isAfter(java.time.Instant.parse(\"2025-01-01T00:00:00Z\"))");
    }

    @Test
    void date_before() {
        String result = translate("customer", "first_purchase_date", "DATE", "before", "2026-01-01T00:00:00Z");
        assertThat(result).contains(".isBefore(java.time.Instant.parse(\"2026-01-01T00:00:00Z\"))");
    }

    @Test
    void date_between() {
        String result = translate("customer", "first_purchase_date", "DATE", "between",
                Map.of("from", "2025-01-01T00:00:00Z", "to", "2026-01-01T00:00:00Z"));
        assertThat(result).contains(".isAfter(java.time.Instant.parse(\"2025-01-01T00:00:00Z\"))");
        assertThat(result).contains(".isBefore(java.time.Instant.parse(\"2026-01-01T00:00:00Z\"))");
    }

    // --- LIST ---

    @Test
    void list_contains() {
        String result = translate("customer", "tags", "LIST", "contains", "vip");
        assertThat(result).contains("((java.util.List) attrs[\"tags\"]).contains(\"vip\")");
    }

    @Test
    void list_notContains() {
        String result = translate("customer", "tags", "LIST", "not_contains", "blocked");
        assertThat(result).contains("!((java.util.List) attrs[\"tags\"]).contains(\"blocked\")");
    }

    @Test
    void list_sizeGte() {
        String result = translate("customer", "tags", "LIST", "size_gte", 2);
        assertThat(result).contains("((java.util.List) attrs[\"tags\"]).size() >= 2");
    }

    // --- error cases ---

    @Test
    void unsupported_dataType_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                translate("customer", "x", "GEO", "equals", "HCM"));
    }

    @Test
    void unsupported_schemaType_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                translate("redemption_metadata", "x", "STRING", "equals", "y"));
    }

    @Test
    void missing_required_param_throws() {
        Map<String, Object> params = new HashMap<>();
        params.put("schema_type", "customer");
        params.put("field_key", "vip_tier");
        // missing data_type
        assertThrows(IllegalArgumentException.class, () ->
                translator.translate("n1", params, "RC"));
    }

    @Test
    void string_unsupportedComparator_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                translate("customer", "vip_tier", "STRING", "between", "platinum"));
    }

    // --- escape ---

    @Test
    void fieldKey_withQuote_escaped() {
        String result = translate("customer", "key\"with\"quote", "STRING", "equals", "val");
        assertThat(result).contains("attrs[\"key\\\"with\\\"quote\"]");
    }

    // --- helper ---

    private String translate(String schemaType, String fieldKey, String dataType,
                             String comparator, Object value) {
        Map<String, Object> params = new HashMap<>();
        params.put("schema_type", schemaType);
        params.put("field_key", fieldKey);
        params.put("data_type", dataType);
        params.put("comparator", comparator);
        params.put("value", value);
        return translator.translate("n1", params, "RC");
    }
}
