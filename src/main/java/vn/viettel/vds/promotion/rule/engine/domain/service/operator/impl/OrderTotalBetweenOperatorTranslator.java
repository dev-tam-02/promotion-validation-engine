package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class OrderTotalBetweenOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object minAmount = params.get("minAmount");
        Object maxAmount = params.get("maxAmount");

        if (minAmount == null) {
            throw new IllegalArgumentException("Missing required parameter 'minAmount' for order.total.between operator");
        }
        if (maxAmount == null) {
            throw new IllegalArgumentException("Missing required parameter 'maxAmount' for order.total.between operator");
        }

        StringBuilder sb = new StringBuilder();
        // Use BigDecimal.compareTo() for proper BigDecimal comparison in Drools
        // total >= minAmount AND total <= maxAmount
        sb.append("        $order: Order(\n");
        sb.append("            total != null,\n");
        sb.append("            total.compareTo(new BigDecimal(\"").append(minAmount).append("\")) >= 0,\n");
        sb.append("            total.compareTo(new BigDecimal(\"").append(maxAmount).append("\")) <= 0\n");
        sb.append("        )\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.total.between";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.total.between".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
