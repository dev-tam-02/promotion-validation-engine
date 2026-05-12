package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class OrderItemPriceBetweenOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object minPrice = params.get("minPrice");
        Object maxPrice = params.get("maxPrice");

        if (minPrice == null || maxPrice == null) {
            throw new IllegalArgumentException("Missing required parameters 'minPrice' and 'maxPrice' for order.item.price.between operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $order: Order()\n");
        sb.append("        exists(OrderItem(price >= ").append(minPrice)
                .append(" && price <= ").append(maxPrice).append(") from $order.getItems())\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.item.price.between";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.item.price.between".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
