package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

/**
 * Operator translator for redemption: code holder only.
 * Ensures only the assigned code holder can redeem the voucher.
 */
@Component
public class RedemptionCodeHolderOnlyOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        StringBuilder sb = new StringBuilder();
        // Check that the redeeming user is the code holder
        sb.append("        $redemption: Redemption(redeemingCodeHolder == true)\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "redemption.code_holder_only";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "redemption.code_holder_only".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
