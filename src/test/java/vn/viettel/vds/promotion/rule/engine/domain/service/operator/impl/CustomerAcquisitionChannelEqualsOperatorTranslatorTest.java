package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerAcquisitionChannelEqualsOperatorTranslatorTest {

    private CustomerAcquisitionChannelEqualsOperatorTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new CustomerAcquisitionChannelEqualsOperatorTranslator();
    }

    // --- metadata ---

    @Test
    void getOperatorName_returnsExpected() {
        assertThat(translator.getOperatorName()).isEqualTo("customer.acquisition_channel.equals");
    }

    @Test
    void getVersion_returns1() {
        assertThat(translator.getVersion()).isEqualTo(1);
    }

    @Test
    void supports_matchingNameAndNullVersion_true() {
        assertThat(translator.supports("customer.acquisition_channel.equals", null)).isTrue();
    }

    @Test
    void supports_matchingNameAndVersion1_true() {
        assertThat(translator.supports("customer.acquisition_channel.equals", 1)).isTrue();
    }

    @Test
    void supports_differentName_false() {
        assertThat(translator.supports("customer.tier.equals", null)).isFalse();
    }

    // --- equals ---

    @Test
    void translate_equals_rendersConstraint() {
        String result = translator.translate("n1", Map.of("comparator", "equals", "value", "paid"), "RC");
        assertThat(result).contains("acquisitionChannel == \"paid\"");
        assertThat(result).contains("$customer: Customer(");
    }

    @Test
    void translate_defaultComparator_usesEquals() {
        // no "comparator" key → default is equals
        String result = translator.translate("n1", Map.of("value", "organic"), "RC");
        assertThat(result).contains("acquisitionChannel == \"organic\"");
    }

    // --- not_equals ---

    @Test
    void translate_notEquals_rendersConstraint() {
        String result = translator.translate("n1", Map.of("comparator", "not_equals", "value", "referral"), "RC");
        assertThat(result).contains("acquisitionChannel != \"referral\"");
    }

    // --- in ---

    @Test
    void translate_in_multipleValues_rendersInClause() {
        String result = translator.translate("n1",
                Map.of("comparator", "in", "value", List.of("paid", "organic")), "RC");
        assertThat(result).contains("acquisitionChannel in (\"paid\", \"organic\")");
    }

    @Test
    void translate_in_singleStringValue_accepted() {
        String result = translator.translate("n1",
                Map.of("comparator", "in", "value", "social"), "RC");
        assertThat(result).contains("acquisitionChannel in (\"social\")");
    }

    // --- not_in ---

    @Test
    void translate_notIn_multipleValues_rendersNotInClause() {
        String result = translator.translate("n1",
                Map.of("comparator", "not_in", "value", List.of("email", "social")), "RC");
        assertThat(result).contains("acquisitionChannel not in (\"email\", \"social\")");
    }

    // --- unsupported comparator ---

    @Test
    void translate_unsupportedComparator_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                translator.translate("n1", Map.of("comparator", "between", "value", "paid"), "RC"));
    }

    // --- escape ---

    @Test
    void translate_valueWithQuote_escapesCorrectly() {
        String result = translator.translate("n1",
                Map.of("comparator", "equals", "value", "channel-\"test\""), "RC");
        assertThat(result).contains("\\\"test\\\"");
    }

    @Test
    void translate_nullValue_treatedAsEmpty() {
        // null value → escape returns "" → acquisitionChannel == ""
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("comparator", "equals");
        params.put("value", null);
        String result = translator.translate("n1", params, "RC");
        assertThat(result).contains("acquisitionChannel == \"\"");
    }

    // --- empty list guard ---

    @Test
    void translate_in_emptyList_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                translator.translate("n1",
                        Map.of("comparator", "in", "value", List.of()), "RC"));
    }
}
