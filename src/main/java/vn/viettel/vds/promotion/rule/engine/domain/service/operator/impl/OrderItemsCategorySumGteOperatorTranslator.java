package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

/**
 * Operator: order.items.category.sum.gte
 * Checks if the total value of items in a specific category is >= threshold.
 * Uses Drools accumulate for aggregation.
 *
 * Params: category (String), minTotal (Number)
 * Example DRL: accumulate(OrderItem(category == "electronics", $p: price * quantity) from $order.getItems(); $sum: sum($p)); eval($sum >= 100000)
 */
@Component
public class OrderItemsCategorySumGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object category = params.get("category");
        Object minTotal = params.get("minTotal");
        if (category == null || minTotal == null) {
            throw new IllegalArgumentException(
                    "Missing required parameters 'category' and 'minTotal' for order.items.category.sum.gte operator");
        }

        return "        $order: Order()\n" +
                "        Number($categorySum: doubleValue >= " + minTotal + ") from accumulate(\n" +
                "            OrderItem(category == \"" + category + "\", $itemTotal: price * quantity) from $order.getItems(),\n" +
                "            sum($itemTotal)\n" +
                "        )\n";
    }

    @Override
    public String getOperatorName() {
        return "order.items.category.sum.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.items.category.sum.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
