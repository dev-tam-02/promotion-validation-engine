package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Operator translator for budget constraint: total orders value.
 * Checks if total orders value is within the configured limit.
 */
@Component
public class BudgetOrdersValueLteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object maxValueParam = params.get("maxValue");
        if (maxValueParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'maxValue' for budget.orders_value.total.lte operator");
        }

        BigDecimal maxValue = parseBigDecimalParam(maxValueParam, "maxValue");
        if (maxValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Parameter 'maxValue' must be positive for budget.orders_value.total.lte operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $limits: LimitsCtx(totalOrdersValue.compareTo(new java.math.BigDecimal(\"")
                .append(maxValue.toPlainString()).append("\")) < 0)\n");

        return sb.toString();
    }

    private BigDecimal parseBigDecimalParam(Object param, String paramName) {
        if (param instanceof BigDecimal bd) {
            return bd;
        }
        if (param instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(param.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + paramName + "' must be a number for budget.orders_value.total.lte operator");
        }
    }

    @Override
    public String getOperatorName() {
        return "budget.orders_value.total.lte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "budget.orders_value.total.lte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
