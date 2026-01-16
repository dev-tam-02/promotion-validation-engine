package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class OrderInitialAmountLteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object amount = params.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("Missing required parameter 'amount' for order.initial.amount.lte operator");
        }

        StringBuilder sb = new StringBuilder();
        // Use BigDecimal.compareTo() for proper BigDecimal comparison in Drools
        // compareTo returns: -1 if less, 0 if equal, 1 if greater
        // So <= 0 means initialAmount is less than or equal to amount
        sb.append("        $order: Order(initialAmount != null, initialAmount.compareTo(new BigDecimal(\"")
                .append(amount)
                .append("\")) <= 0)\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.initial.amount.lte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.initial.amount.lte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
