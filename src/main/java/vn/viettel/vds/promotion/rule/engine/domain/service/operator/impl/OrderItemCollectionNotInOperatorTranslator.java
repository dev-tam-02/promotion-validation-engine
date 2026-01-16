package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;

@Component
public class OrderItemCollectionNotInOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object collections = params.get("collections");
        if (collections == null) {
            throw new IllegalArgumentException("Missing required parameter 'collections' for order.item.collection.not_in operator");
        }

        List<String> collectionList = (List<String>) collections;
        StringBuilder sb = new StringBuilder();
        // Check that NO items belong to the excluded collections
        sb.append("        not exists(OrderItem(collection in (");
        for (int i = 0; i < collectionList.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(collectionList.get(i)).append("\"");
        }
        sb.append(")) from $order.items)\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.item.collection.not_in";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.item.collection.not_in".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
