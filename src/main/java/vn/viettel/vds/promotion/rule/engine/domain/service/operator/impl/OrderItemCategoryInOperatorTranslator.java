package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderItemCategoryInOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object categories = params.get("categories");
        if (categories == null) {
            throw new IllegalArgumentException("Missing required parameter 'categories' for order.item.category.in operator");
        }

        List<?> categoryList = (List<?>) categories;
        String categoriesStr = categoryList.stream()
                .map(cat -> "\"" + cat + "\"")
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("        $order: Order()\n");
        sb.append("        exists(OrderItem(category in (").append(categoriesStr).append(")) from $order.getItems())\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.item.category.in";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.item.category.in".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}