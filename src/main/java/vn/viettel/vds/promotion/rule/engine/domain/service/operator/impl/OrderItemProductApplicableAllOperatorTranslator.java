package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Operator translator for product pattern: every order item must match.
 * Uses forall pattern - all items must be in the specified product list.
 */
@Component
public class OrderItemProductApplicableAllOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object productsParam = params.get("products");
        if (productsParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'products' for order.item.product.applicable.all operator");
        }

        List<String> products = parseListParam(productsParam, "products");
        if (products.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'products' cannot be empty for order.item.product.applicable.all operator");
        }

        String productList = products.stream()
                .map(p -> "\"" + p + "\"")
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        // Check that all items have productId in the list
        sb.append("        $order: Order(items.size() > 0)\n");
        sb.append("        forall(OrderItem(productId memberOf java.util.Arrays.asList(").append(productList).append(")) from $order.getItems())\n");

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
        throw new IllegalArgumentException("Parameter '" + paramName + "' must be a list for order.item.product.applicable.all operator");
    }

    @Override
    public String getOperatorName() {
        return "order.item.product.applicable.all";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.item.product.applicable.all".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
