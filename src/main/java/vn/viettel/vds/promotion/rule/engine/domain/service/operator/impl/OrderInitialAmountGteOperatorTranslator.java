package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class OrderInitialAmountGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object minAmount = params.get("minAmount");
        if (minAmount == null) {
            throw new IllegalArgumentException("Missing required parameter 'minAmount' for order.initial.amount.gte operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $order: Order(initialAmount >= ").append(minAmount).append(")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.initial.amount.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.initial.amount.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
