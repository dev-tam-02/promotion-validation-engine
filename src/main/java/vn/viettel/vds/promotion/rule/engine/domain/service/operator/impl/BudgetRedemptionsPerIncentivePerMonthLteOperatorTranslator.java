package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

/**
 * Counter-policy translator for {@code budget.redemptions.per_incentive.per_month.lte}.
 *
 * <p>Bucket scope: {@code BucketKeys.incentivePerMonth(incentiveId, $now)}.
 */
@Component
public class BudgetRedemptionsPerIncentivePerMonthLteOperatorTranslator implements OperatorTranslator {

    private static final String OPERATOR_NAME = "budget.redemptions.per_incentive.per_month.lte";
    private static final String POLICY_NAME = "redemptions_per_incentive_per_month";

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
        return "$candidate: Candidate()";
    }

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        long limit = extractLimit(params);
        return String.format(
                "result.addPolicy(new vn.viettel.vds.promotion.rule.engine.domain.model.QuotaPolicy(" +
                        "\"%s\", %dL, " +
                        "vn.viettel.vds.promotion.rule.engine.application.service.BucketKeys" +
                        ".incentivePerMonth($candidate.getId(), java.time.Instant.now())));",
                POLICY_NAME, limit);
    }

    private long extractLimit(Map<String, Object> params) {
        Object v = params.get("value");
        if (v == null) v = params.get("maxValue");
        if (v == null) throw new IllegalArgumentException("Missing 'value' param for " + OPERATOR_NAME);
        return ((Number) v).longValue();
    }
}
