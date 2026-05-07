package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

/**
 * Counter-policy translator for {@code budget.redemptions.per_customer.in_campaign.lte}.
 *
 * <p>Bucket scope: {@code BucketKeys.customerInCampaign(customerId, campaignId)} — sticky, no time window.
 */
@Component
public class BudgetRedemptionsPerCustomerInCampaignLteOperatorTranslator implements OperatorTranslator {

    private static final String OPERATOR_NAME = "budget.redemptions.per_customer.in_campaign.lte";
    private static final String POLICY_NAME = "redemptions_per_customer_in_campaign";

    @Override
    public String getOperatorName() {
        return OPERATOR_NAME;
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return OPERATOR_NAME.equals(operatorName) && (version == null || version.equals(getVersion()));
    }

    @Override
    public boolean isCounterPolicy() {
        return true;
    }

    @Override
    public String getCounterFactPattern() {
        return "$c: Customer()";
    }

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        long limit = extractLimit(params);
        String campaignId = extractCampaignId(params);
        return String.format(
                "result.addPolicy(new vn.viettel.vds.promotion.rule.engine.domain.model.QuotaPolicy(" +
                        "\"%s\", %dL, " +
                        "vn.viettel.vds.promotion.rule.engine.application.service.BucketKeys" +
                        ".customerInCampaign($c.getId(), \"%s\")));",
                POLICY_NAME, limit, escape(campaignId));
    }

    private long extractLimit(Map<String, Object> params) {
        Object v = params.get("value");
        if (v == null) v = params.get("maxValue");
        if (v == null) throw new IllegalArgumentException("Missing 'value' param for " + OPERATOR_NAME);
        return ((Number) v).longValue();
    }

    private String extractCampaignId(Map<String, Object> params) {
        Object v = params.get("campaignId");
        return v != null ? v.toString() : "";
    }

    private String escape(String v) {
        return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
