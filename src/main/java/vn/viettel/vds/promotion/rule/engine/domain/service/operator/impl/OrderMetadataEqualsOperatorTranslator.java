package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class OrderMetadataEqualsOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object key = params.get("key");
        Object value = params.get("value");
        
        if (key == null || value == null) {
            throw new IllegalArgumentException("Missing required parameters 'key' and 'value' for order.metadata.equals operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $order: Order(metadata[\"").append(key).append("\"] == \"").append(value).append("\")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.metadata.equals";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.metadata.equals".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
