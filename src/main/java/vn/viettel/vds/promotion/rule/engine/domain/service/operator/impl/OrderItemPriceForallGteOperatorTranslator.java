package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Operator translator for product pattern: price of each item >= value.
 * Uses forall pattern - all items must have price >= specified value.
 */
@Component
public class OrderItemPriceForallGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object minPriceParam = params.get("minPrice");
        if (minPriceParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'minPrice' for order.item.price.forall.gte operator");
        }

        double minPrice = parseDoubleParam(minPriceParam, "minPrice");
        if (minPrice < 0) {
            throw new IllegalArgumentException("Parameter 'minPrice' cannot be negative for order.item.price.forall.gte operator");
        }

        StringBuilder sb = new StringBuilder();
        // Check that all items have price >= minPrice
        sb.append("        $order: Order(items.size() > 0)\n");
        sb.append("        forall(OrderItem(price >= ").append(minPrice).append(") from $order.items)\n");

        return sb.toString();
    }

    private double parseDoubleParam(Object param, String paramName) {
        if (param instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(param.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + paramName + "' must be a number for order.item.price.forall.gte operator");
        }
    }

    @Override
    public String getOperatorName() {
        return "order.item.price.forall.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.item.price.forall.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
