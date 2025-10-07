package vn.viettel.vds.promotion.validation.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class CustomerTierEqualsOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object tier = params.get("tier");
        if (tier == null) {
            throw new IllegalArgumentException("Missing required parameter 'tier' for customer.tier.equals operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $customer: Customer(tier == \"").append(tier).append("\")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "customer.tier.equals";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "customer.tier.equals".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}