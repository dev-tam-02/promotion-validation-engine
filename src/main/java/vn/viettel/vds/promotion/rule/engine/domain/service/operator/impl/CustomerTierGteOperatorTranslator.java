package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomerTierGteOperatorTranslator implements OperatorTranslator {

    private static final List<String> DEFAULT_TIER_ORDER = List.of(
            "bronze", "silver", "gold", "platinum", "diamond"
    );

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object tierParam = params.get("tier");
        if (tierParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'tier' for customer.tier.gte operator");
        }
        String targetTier = tierParam.toString().toLowerCase();

        // Allow custom tier order or use default
        List<String> tierOrder;
        Object tiersOrderParam = params.get("tiers_order");
        if (tiersOrderParam instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> customOrder = ((List<String>) tiersOrderParam).stream()
                    .map(String::toLowerCase)
                    .toList();
            tierOrder = customOrder;
        } else {
            tierOrder = DEFAULT_TIER_ORDER;
        }

        // Build tier order list as Java code
        String tierListCode = tierOrder.stream()
                .map(t -> "\"" + t + "\"")
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("        $customer: Customer(\n");
        sb.append("            tier != null &&\n");
        sb.append("            eval({\n");
        sb.append("                java.util.List tierOrder = java.util.Arrays.asList(").append(tierListCode).append(");\n");
        sb.append("                int customerTierIndex = tierOrder.indexOf(tier.toLowerCase());\n");
        sb.append("                int targetTierIndex = tierOrder.indexOf(\"").append(targetTier).append("\");\n");
        sb.append("                customerTierIndex >= 0 && targetTierIndex >= 0 && customerTierIndex >= targetTierIndex;\n");
        sb.append("            })\n");
        sb.append("        )\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "customer.tier.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "customer.tier.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
