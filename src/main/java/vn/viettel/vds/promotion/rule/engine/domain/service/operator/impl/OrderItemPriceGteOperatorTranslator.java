package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class OrderItemPriceGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object minPrice = params.get("minPrice");
        if (minPrice == null) {
            throw new IllegalArgumentException("Missing required parameter 'minPrice' for order.item.price.gte operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $order: Order()\n");
        sb.append("        exists(OrderItem(price >= ").append(minPrice).append(") from $order.getItems())\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.item.price.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.item.price.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
