package vn.viettel.vds.promotion.validation.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class OrderTotalGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object amount = params.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("Missing required parameter 'amount' for order.total.gte operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $order: Order(total >= ").append(amount).append(")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.total.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.total.gte".equals(operatorName) &&
               (version == null || version.equals(getVersion()));
    }
}