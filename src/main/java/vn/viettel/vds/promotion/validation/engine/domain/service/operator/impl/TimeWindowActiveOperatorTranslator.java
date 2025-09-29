package vn.viettel.vds.promotion.validation.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;

import java.util.Map;

@Component
public class TimeWindowActiveOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object policyIdParam = params.get("policyId");
        if (policyIdParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'policyId' for time.window.active operator");
        }

        String policyId = policyIdParam.toString();

        // Optional timezone parameter - default to system timezone if not specified
        Object tzParam = params.get("tz");
        String timezone = tzParam != null ? tzParam.toString() : "UTC";

        // Generate Drools condition that uses a time window validation service
        // This assumes there's a global timeWindowService available in the Drools session
        StringBuilder sb = new StringBuilder();
        sb.append("        eval(timeWindowService.isActiveNow(\"").append(policyId).append("\", \"").append(timezone).append("\"))\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "time.window.active";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "time.window.active".equals(operatorName) &&
               (version == null || version.equals(getVersion()));
    }
}