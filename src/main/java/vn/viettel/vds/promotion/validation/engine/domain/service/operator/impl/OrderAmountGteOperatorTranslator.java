package vn.viettel.vds.promotion.validation.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class OrderAmountGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object amountParam = params.get("amount");
        if (amountParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'amount' for order.amount.gte operator");
        }

        Number amount;
        if (amountParam instanceof Number number) {
            amount = number;
        } else {
            try {
                amount = Double.parseDouble(amountParam.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter 'amount' must be a number for order.amount.gte operator");
            }
        }

        // Optional currency parameter - default to any currency if not specified
        Object currencyParam = params.get("currency");
        String currency = currencyParam != null ? currencyParam.toString() : null;

        StringBuilder sb = new StringBuilder();
        if (currency != null) {
            // If currency is specified, check both amount and currency
            // Use BigDecimal.compareTo() for proper BigDecimal comparison
            sb.append("        $order: Order(total != null, total.compareTo(new BigDecimal(\"")
              .append(amount)
              .append("\")) >= 0, currency == \"")
              .append(currency)
              .append("\")\n");
        } else {
            // If currency not specified, only check amount
            // Use BigDecimal.compareTo() for proper BigDecimal comparison
            sb.append("        $order: Order(total != null, total.compareTo(new BigDecimal(\"")
              .append(amount)
              .append("\")) >= 0)\n");
        }

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.amount.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.amount.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}