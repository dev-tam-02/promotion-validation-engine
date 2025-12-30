package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

/**
 * Operator translator for budget constraint: redemptions per customer in campaign.
 * Checks if per-customer redemptions is within the configured limit.
 */
@Component
public class BudgetRedemptionsPerCustomerLteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object maxValueParam = params.get("maxValue");
        if (maxValueParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'maxValue' for budget.redemptions.per_customer.lte operator");
        }

        int maxValue = parseIntParam(maxValueParam, "maxValue");
        if (maxValue < 1) {
            throw new IllegalArgumentException("Parameter 'maxValue' must be positive for budget.redemptions.per_customer.lte operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $limits: LimitsCtx(perCustomerUsed < ").append(maxValue).append(")\n");

        return sb.toString();
    }

    private int parseIntParam(Object param, String paramName) {
        if (param instanceof Integer integer) {
            return integer;
        }
        try {
            return Integer.parseInt(param.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + paramName + "' must be an integer for budget.redemptions.per_customer.lte operator");
        }
    }

    @Override
    public String getOperatorName() {
        return "budget.redemptions.per_customer.lte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "budget.redemptions.per_customer.lte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
