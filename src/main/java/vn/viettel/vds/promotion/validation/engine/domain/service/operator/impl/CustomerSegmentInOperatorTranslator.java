package vn.viettel.vds.promotion.validation.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomerSegmentInOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object segmentsParam = params.get("segments");
        if (segmentsParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'segments' for customer.segment.in operator");
        }

        List<String> segments;
        if (segmentsParam instanceof List) {
            segments = (List<String>) segmentsParam;
        } else if (segmentsParam instanceof String) {
            // Handle single segment as a list
            segments = List.of((String) segmentsParam);
        } else {
            throw new IllegalArgumentException("Parameter 'segments' must be a List<String> or String for customer.segment.in operator");
        }

        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'segments' cannot be empty for customer.segment.in operator");
        }

        // Generate Drools condition to check if any of the customer's segments matches any of the target segments
        StringBuilder sb = new StringBuilder();

        if (segments.size() == 1) {
            // Single segment check - use contains for efficiency
            sb.append("        $customer: Customer(segments contains \"").append(segments.get(0)).append("\")\n");
        } else {
            // Multiple segments - check if any customer segment is in the target list
            String segmentList = segments.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(", "));

            sb.append("        $customer: Customer(\n");
            sb.append("            segments != null && segments.size() > 0 &&\n");
            sb.append("            eval(segments.stream().anyMatch(seg -> java.util.Arrays.asList(")
              .append(segmentList).append(").contains(seg)))\n");
            sb.append("        )\n");
        }

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "customer.segment.in";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "customer.segment.in".equals(operatorName) &&
               (version == null || version.equals(getVersion()));
    }
}