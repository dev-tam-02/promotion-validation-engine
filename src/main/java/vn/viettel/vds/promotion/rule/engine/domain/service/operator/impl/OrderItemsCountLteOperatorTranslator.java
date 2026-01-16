package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class OrderItemsCountLteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object countParam = params.get("count");
        if (countParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'count' for order.items.count.lte operator");
        }

        Integer count;
        if (countParam instanceof Integer integer) {
            count = integer;
        } else {
            try {
                count = Integer.parseInt(countParam.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter 'count' must be an integer for order.items.count.lte operator");
            }
        }

        if (count < 0) {
            throw new IllegalArgumentException("Parameter 'count' must be non-negative for order.items.count.lte operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $order: Order(itemsQuantity <= ").append(count).append(")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.items.count.lte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.items.count.lte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
