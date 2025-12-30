package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

/**
 * Operator translator for redemption metadata.
 * Checks if redemption metadata field equals a specific value.
 */
@Component
public class RedemptionMetadataEqualsOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object keyParam = params.get("key");
        if (keyParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'key' for redemption.metadata.equals operator");
        }

        Object valueParam = params.get("value");
        if (valueParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'value' for redemption.metadata.equals operator");
        }

        String key = keyParam.toString();
        String value = valueParam.toString();

        StringBuilder sb = new StringBuilder();
        sb.append("        $redemption: Redemption(metadata[\"").append(key).append("\"] == \"").append(value).append("\")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "redemption.metadata.equals";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "redemption.metadata.equals".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
