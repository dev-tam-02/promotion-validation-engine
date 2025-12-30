package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class CustomerLifetimeValueGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object minValue = params.get("minValue");
        if (minValue == null) {
            throw new IllegalArgumentException("Missing required parameter 'minValue' for customer.lifetime.value.gte operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $customer: Customer(lifetimeValue >= ").append(minValue).append(")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "customer.lifetime.value.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "customer.lifetime.value.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
