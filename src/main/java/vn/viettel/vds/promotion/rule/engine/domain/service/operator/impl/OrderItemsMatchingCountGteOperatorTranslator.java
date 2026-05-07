package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Operator: order.items.matching.count.gte
 * Checks if at least N items match a given category/brand filter.
 * Uses Drools accumulate for counting.
 * <p>
 * Params: minCount (Integer), categories (List, optional), brands (List, optional)
 */
@Component
public class OrderItemsMatchingCountGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object minCount = params.get("minCount");
        if (minCount == null) {
            throw new IllegalArgumentException("Missing required parameter 'minCount' for order.items.matching.count.gte operator");
        }

        // Build item filter condition
        StringBuilder itemFilter = new StringBuilder();
        Object categories = params.get("categories");
        Object brands = params.get("brands");

        if (categories instanceof List<?> catList && !catList.isEmpty()) {
            String catStr = catList.stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(", "));
            itemFilter.append("category in (").append(catStr).append(")");
        }
        if (brands instanceof List<?> brandList && !brandList.isEmpty()) {
            if (!itemFilter.isEmpty()) itemFilter.append(", ");
            String brandStr = brandList.stream().map(b -> "\"" + b + "\"").collect(Collectors.joining(", "));
            itemFilter.append("brand in (").append(brandStr).append(")");
        }

        if (itemFilter.isEmpty()) {
            // No filter = count all items
            itemFilter.append("$i: OrderItem()");
        } else {
            itemFilter.insert(0, "$i: OrderItem(");
            itemFilter.append(")");
        }

        return "        $order: Order()\n" +
                "        Number(intValue >= " + minCount + ") from accumulate(\n" +
                "            " + itemFilter + " from $order.getItems(),\n" +
                "            count($i)\n" +
                "        )\n";
    }

    @Override
    public String getOperatorName() {
        return "order.items.matching.count.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.items.matching.count.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
