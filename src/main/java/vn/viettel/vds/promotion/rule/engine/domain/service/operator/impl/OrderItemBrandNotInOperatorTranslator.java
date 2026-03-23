package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;

@Component
public class OrderItemBrandNotInOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object brands = params.get("brands");
        if (brands == null) {
            throw new IllegalArgumentException("Missing required parameter 'brands' for order.item.brand.not_in operator");
        }

        List<String> brandList = (List<String>) brands;
        StringBuilder sb = new StringBuilder();
        sb.append("        $order: Order()\n");
        // Check that NO items belong to the excluded brands
        sb.append("        not exists(OrderItem(brand in (");
        for (int i = 0; i < brandList.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(brandList.get(i)).append("\"");
        }
        sb.append(")) from $order.getItems())\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.item.brand.not_in";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.item.brand.not_in".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
