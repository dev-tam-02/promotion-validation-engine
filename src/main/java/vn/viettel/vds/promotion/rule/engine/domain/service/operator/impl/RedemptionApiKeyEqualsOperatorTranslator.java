package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Operator translator for redemption: redeeming API key.
 * Checks if the redeeming API key is in the allowed list.
 */
@Component
public class RedemptionApiKeyEqualsOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object apiKeysParam = params.get("apiKeys");
        if (apiKeysParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'apiKeys' for redemption.api_key.equals operator");
        }

        List<String> apiKeys = parseListParam(apiKeysParam, "apiKeys");
        if (apiKeys.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'apiKeys' cannot be empty for redemption.api_key.equals operator");
        }

        String apiKeyList = apiKeys.stream()
                .map(k -> "\"" + k + "\"")
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("        $redemption: Redemption(apiKey memberOf java.util.Arrays.asList(").append(apiKeyList).append("))\n");

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseListParam(Object param, String paramName) {
        if (param instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .toList();
        }
        if (param instanceof String str) {
            return List.of(str.split(",")).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        throw new IllegalArgumentException("Parameter '" + paramName + "' must be a list for redemption.api_key.equals operator");
    }

    @Override
    public String getOperatorName() {
        return "redemption.api_key.equals";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "redemption.api_key.equals".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
