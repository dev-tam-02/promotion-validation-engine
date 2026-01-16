package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;

@Component
public class OrderItemSkuInOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object skus = params.get("skus");
        if (skus == null) {
            throw new IllegalArgumentException("Missing required parameter 'skus' for order.item.sku.in operator");
        }

        List<String> skuList = (List<String>) skus;
        StringBuilder sb = new StringBuilder();
        sb.append("        exists(OrderItem(sku in (");
        for (int i = 0; i < skuList.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(skuList.get(i)).append("\"");
        }
        sb.append(")) from $order.items)\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.item.sku.in";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.item.sku.in".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
