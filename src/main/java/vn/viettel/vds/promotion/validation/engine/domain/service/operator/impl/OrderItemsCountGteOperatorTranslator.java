package vn.viettel.vds.promotion.validation.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class OrderItemsCountGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object countParam = params.get("count");
        if (countParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'count' for order.items.count.gte operator");
        }

        Integer count;
        if (countParam instanceof Integer) {
            count = (Integer) countParam;
        } else {
            try {
                count = Integer.parseInt(countParam.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter 'count' must be an integer for order.items.count.gte operator");
            }
        }

        if (count < 0) {
            throw new IllegalArgumentException("Parameter 'count' must be non-negative for order.items.count.gte operator");
        }

        StringBuilder sb = new StringBuilder();
        // Use itemsQuantity field if available, otherwise use items.size()
        sb.append("        $order: Order(itemsQuantity >= ").append(count).append(")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.items.count.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.items.count.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}