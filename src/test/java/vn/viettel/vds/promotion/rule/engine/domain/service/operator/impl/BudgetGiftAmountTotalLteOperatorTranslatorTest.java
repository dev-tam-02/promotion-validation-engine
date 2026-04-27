package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BudgetGiftAmountTotalLteOperatorTranslatorTest {

    private BudgetGiftAmountTotalLteOperatorTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new BudgetGiftAmountTotalLteOperatorTranslator();
    }

    // --- metadata ---

    @Test
    void getOperatorName_returnsExpected() {
        assertThat(translator.getOperatorName()).isEqualTo("budget.gift_amount.total.lte");
    }

    @Test
    void getVersion_returns1() {
        assertThat(translator.getVersion()).isEqualTo(1);
    }

    @Test
    void supports_matchingNameNullVersion_true() {
        assertThat(translator.supports("budget.gift_amount.total.lte", null)).isTrue();
    }

    @Test
    void supports_matchingNameVersion1_true() {
        assertThat(translator.supports("budget.gift_amount.total.lte", 1)).isTrue();
    }

    @Test
    void supports_differentName_false() {
        assertThat(translator.supports("budget.discounted_amount.total.lte", null)).isFalse();
    }

    // --- translate ---

    @Test
    void translate_bigDecimalValue_rendersDrlPattern() {
        String result = translator.translate("n1", Map.of("maxValue", new BigDecimal("100000.00")), "RC");
        assertThat(result).contains("$limits: LimitsCtx(");
        assertThat(result).contains("totalGiftAmount.compareTo(new java.math.BigDecimal(\"100000.00\")) < 0");
    }

    @Test
    void translate_integerValue_parsedCorrectly() {
        String result = translator.translate("n1", Map.of("maxValue", 50000), "RC");
        assertThat(result).contains("totalGiftAmount.compareTo(new java.math.BigDecimal(\"");
        assertThat(result).contains(") < 0");
    }

    @Test
    void translate_stringValue_parsedCorrectly() {
        String result = translator.translate("n1", Map.of("maxValue", "200000"), "RC");
        assertThat(result).contains("totalGiftAmount.compareTo(new java.math.BigDecimal(\"200000\")) < 0");
    }

    // --- validation ---

    @Test
    void translate_missingMaxValue_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                translator.translate("n1", Map.of(), "RC"));
    }

    @Test
    void translate_nullMaxValue_throws() {
        Map<String, Object> params = new HashMap<>();
        params.put("maxValue", null);
        assertThrows(IllegalArgumentException.class, () ->
                translator.translate("n1", params, "RC"));
    }

    @Test
    void translate_zeroMaxValue_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                translator.translate("n1", Map.of("maxValue", BigDecimal.ZERO), "RC"));
    }

    @Test
    void translate_negativeMaxValue_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                translator.translate("n1", Map.of("maxValue", new BigDecimal("-1")), "RC"));
    }

    @Test
    void translate_nonNumericString_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                translator.translate("n1", Map.of("maxValue", "abc"), "RC"));
    }
}
