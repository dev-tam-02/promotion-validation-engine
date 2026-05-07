package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class CustomerLoyaltyPointsLteOperatorTranslator implements OperatorTranslator {

    private static final String OPERATOR_NAME = "customer.loyalty.points.lte";

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object pointsParam = params.get("points");
        if (pointsParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'points' for " + OPERATOR_NAME);
        }

        Integer points;
        if (pointsParam instanceof Integer integer) {
            points = integer;
        } else {
            try {
                points = Integer.parseInt(pointsParam.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter 'points' must be an integer for " + OPERATOR_NAME);
            }
        }

        if (points < 0) {
            throw new IllegalArgumentException("Parameter 'points' must be non-negative for " + OPERATOR_NAME);
        }

        return "        $customer: Customer(loyaltyPoints <= " + points + ")\n";
    }

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
}
