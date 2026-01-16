package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomerNotInSegmentOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        Object segmentsParam = params.get("segments");
        if (segmentsParam == null) {
            throw new IllegalArgumentException("Missing required parameter 'segments' for customer.not_in_segment operator");
        }

        List<String> segments;
        if (segmentsParam instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> castedList = (List<String>) segmentsParam;
            segments = castedList;
        } else if (segmentsParam instanceof String str) {
            segments = List.of(str);
        } else {
            throw new IllegalArgumentException("Parameter 'segments' must be a List<String> or String for customer.not_in_segment operator");
        }

        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'segments' cannot be empty for customer.not_in_segment operator");
        }

        StringBuilder sb = new StringBuilder();

        if (segments.size() == 1) {
            // Single segment check - customer must NOT contain this segment
            sb.append("        $customer: Customer(segments not contains \"").append(segments.get(0)).append("\")\n");
        } else {
            // Multiple segments - check that NO customer segment is in the target list
            String segmentList = segments.stream()
                    .map(s -> "\"" + s + "\"")
                    .collect(Collectors.joining(", "));

            sb.append("        $customer: Customer(\n");
            sb.append("            segments == null || segments.size() == 0 ||\n");
            sb.append("            eval(segments.stream().noneMatch(seg -> java.util.Arrays.asList(")
                    .append(segmentList).append(").contains(seg)))\n");
            sb.append("        )\n");
        }

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "customer.not_in_segment";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "customer.not_in_segment".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
