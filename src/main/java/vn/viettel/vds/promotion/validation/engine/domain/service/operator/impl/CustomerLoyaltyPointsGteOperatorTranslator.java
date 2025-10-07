package vn.viettel.vds.promotion.validation.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class CustomerLoyaltyPointsGteOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object pointsParam = params.get("points");
        if (pointsParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'points' for customer.loyalty.points.gte operator");
        }

        Integer points;
        if (pointsParam instanceof Integer) {
            points = (Integer) pointsParam;
        } else {
            try {
                points = Integer.parseInt(pointsParam.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Parameter 'points' must be an integer for customer.loyalty.points.gte operator");
            }
        }

        if (points < 0) {
            throw new IllegalArgumentException("Parameter 'points' must be non-negative for customer.loyalty.points.gte operator");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        $customer: Customer(loyaltyPoints >= ").append(points).append(")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "customer.loyalty.points.gte";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "customer.loyalty.points.gte".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}