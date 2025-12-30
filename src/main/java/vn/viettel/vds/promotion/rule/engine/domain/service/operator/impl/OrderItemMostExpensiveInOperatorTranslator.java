package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Operator translator for product pattern: most expensive item in list.
 * Checks if the most expensive order item's productId is in the specified list.
 */
@Component
public class OrderItemMostExpensiveInOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object productsParam = params.get("products");
        if (productsParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'products' for order.item.most_expensive.in operator");
        }

        List<String> products = parseListParam(productsParam, "products");
        if (products.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'products' cannot be empty for order.item.most_expensive.in operator");
        }

        String productList = products.stream()
                .map(p -> "\"" + p + "\"")
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        // Use helper method in Order to get most expensive item
        sb.append("        $order: Order(mostExpensiveItem != null, mostExpensiveItem.productId memberOf java.util.Arrays.asList(").append(productList).append("))\n");

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseListParam(Object param, String paramName) {
        if (param instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        if (param instanceof String str) {
            return List.of(str.split(",")).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        throw new IllegalArgumentException("Parameter '" + paramName + "' must be a list for order.item.most_expensive.in operator");
    }

    @Override
    public String getOperatorName() {
        return "order.item.most_expensive.in";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.item.most_expensive.in".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
