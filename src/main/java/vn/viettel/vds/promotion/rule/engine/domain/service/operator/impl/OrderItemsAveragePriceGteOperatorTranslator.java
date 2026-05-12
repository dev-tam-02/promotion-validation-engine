package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

/**
 * Operator: order.items.average.price.gte
 * Checks if the average price of order items is >= threshold.
 * Uses Drools accumulate for averaging.
 * <p>
 * Params: minAveragePrice (Number)
 */
@Component
public class OrderItemsAveragePriceGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object minAvg = params.get("minAveragePrice");
        if (minAvg == null) {
            throw new IllegalArgumentException(
                    "Missing required parameter 'minAveragePrice' for order.items.average.price.gte operator");
        }

        return "        $order: Order()\n" +
                "        Number(doubleValue >= " + minAvg + ") from accumulate(\n" +
                "            OrderItem($p: price) from $order.getItems(),\n" +
                "            average($p)\n" +
                "        )\n";
    }

    @Override
    public String getOperatorName() {
        return "order.items.average.price.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.items.average.price.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
