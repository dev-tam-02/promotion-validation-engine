package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Operator translator for budget constraint: total redeemed gift amount per customer per campaign.
 *
 * <p>Renders a Drools {@code LimitsCtx} fact constraint on the {@code totalGiftAmount} field.
 * Passes when {@code totalGiftAmount < maxValue} (i.e. customer is still under the gift cap).
 *
 * <p><b>Decision R-T7 (Option resolution)</b>: Design spec offered two options:
 * <ul>
 *   <li>Option A — reuse {@code Customer.totalDiscountedAmount} (no model change needed)</li>
 *   <li>Option B — use a dedicated gift-amount field to distinguish gifts from discounts</li>
 * </ul>
 * {@code LimitsCtx} already carries {@code totalGiftAmount} (BigDecimal, initialised to ZERO),
 * which provides Option-B semantics without any POJO change. This translator uses
 * {@code LimitsCtx.totalGiftAmount} as the field source (effective Option B — resolved in code).
 * If product team later requires a different semantic, this translator is the single point of change.
 *
 * <p>Required param: {@code maxValue} — the gift-amount ceiling (positive BigDecimal / Number / String).
 */
@Component
public class BudgetGiftAmountTotalLteOperatorTranslator implements OperatorTranslator {

    private static final String OPERATOR_NAME = "budget.gift_amount.total.lte";

    @Override
    public String getOperatorName() {
        return OPERATOR_NAME;
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return OPERATOR_NAME.equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object maxValueParam = params.get("maxValue");
        if (maxValueParam == null) {
            throw new IllegalArgumentException(
                    "Missing required parameter 'maxValue' for " + OPERATOR_NAME);
        }

        BigDecimal maxValue = parseBigDecimal(maxValueParam, "maxValue");
        if (maxValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Parameter 'maxValue' must be positive for " + OPERATOR_NAME);
        }

        return "        $limits: LimitsCtx(totalGiftAmount.compareTo(new java.math.BigDecimal(\""
                + maxValue.toPlainString() + "\")) < 0)\n";
    }

    private BigDecimal parseBigDecimal(Object param, String paramName) {
        if (param instanceof BigDecimal bd) {
            return bd;
        }
        if (param instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(param.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Parameter '" + paramName + "' must be a valid number for " + OPERATOR_NAME);
        }
    }
}
