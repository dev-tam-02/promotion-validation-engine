package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class CustomerUsageCountLtOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object maxUsageParam = params.get("maxUsage");
        if (maxUsageParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'maxUsage' for customer.usage.count.lt operator");
        }

        Object voucherCodeParam = params.get("voucherCode");
        if (voucherCodeParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'voucherCode' for customer.usage.count.lt operator");
        }

        Integer maxUsage;
        if (maxUsageParam instanceof Integer integer) {
            maxUsage = integer;
        } else {
            try {
                maxUsage = Integer.parseInt(maxUsageParam.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter 'maxUsage' must be an integer for customer.usage.count.lt operator");
            }
        }

        if (maxUsage < 1) {
            throw new IllegalArgumentException("Parameter 'maxUsage' must be positive for customer.usage.count.lt operator");
        }

        String voucherCode = voucherCodeParam.toString();

        StringBuilder sb = new StringBuilder();
        // This operator checks usage count for specific voucher code using a service call
        // Assumes there's a global usageService available in Drools session
        sb.append("        eval(usageService.getUsageCount($customer.getId(), \"")
                .append(voucherCode).append("\") < ").append(maxUsage).append(")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "customer.usage.count.lt";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "customer.usage.count.lt".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}