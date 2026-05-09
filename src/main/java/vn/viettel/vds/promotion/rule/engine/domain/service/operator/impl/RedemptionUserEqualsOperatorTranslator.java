package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Operator translator for redemption: redeeming user.
 * Checks if the redeeming user is in the allowed list.
 */
@Component
public class RedemptionUserEqualsOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object userIdsParam = params.get("userIds");
        if (userIdsParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'userIds' for redemption.user.equals operator");
        }

        List<String> userIds = parseListParam(userIdsParam, "userIds");
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'userIds' cannot be empty for redemption.user.equals operator");
        }

        String userIdList = userIds.stream()
                .map(u -> "\"" + u + "\"")
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("        $redemption: Redemption(userId memberOf java.util.Arrays.asList(").append(userIdList).append("))\n");

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
        throw new IllegalArgumentException("Parameter '" + paramName + "' must be a list for redemption.user.equals operator");
    }

    @Override
    public String getOperatorName() {
        return "redemption.user.equals";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "redemption.user.equals".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
