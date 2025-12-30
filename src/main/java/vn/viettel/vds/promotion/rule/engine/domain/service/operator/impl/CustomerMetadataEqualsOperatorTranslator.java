package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class CustomerMetadataEqualsOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object key = params.get("key");
        Object value = params.get("value");
        
        if (key == null || value == null) {
            throw new IllegalArgumentException("Missing required parameters 'key' and 'value' for customer.metadata.equals operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $customer: Customer(attrs[\"").append(key).append("\"] == \"").append(value).append("\")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "customer.metadata.equals";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "customer.metadata.equals".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
