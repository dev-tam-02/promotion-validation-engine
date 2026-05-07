package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;

@Component
public class CustomerSegmentInOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object segmentsParam = params.get("segments");
        if (segmentsParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'segments' for customer.in_segment operator");
        }

        List<String> segments;
        if (segmentsParam instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> castedList = (List<String>) segmentsParam;
            segments = castedList;
        } else if (segmentsParam instanceof String str) {
            // Handle single segment as a list
            segments = List.of(str);
        } else {
            throw new IllegalArgumentException("Parameter 'segments' must be a List<String> or String for customer.in_segment operator");
        }

        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'segments' cannot be empty for customer.in_segment operator");
        }

        // Generate Drools condition to check if any of the customer's segments matches any of the target segments
        StringBuilder sb = new StringBuilder();

        if (segments.size() == 1) {
            sb.append("        $customer: Customer(segments contains \"").append(segments.get(0)).append("\")\n");
        } else {
            // Multiple segments — use OR of contains checks (Drools-compatible)
            sb.append("        $customer: Customer(");
            for (int i = 0; i < segments.size(); i++) {
                if (i > 0) sb.append(" || ");
                sb.append("segments contains \"").append(segments.get(i)).append("\"");
            }
            sb.append(")\n");
        }

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "customer.in_segment";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "customer.in_segment".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}