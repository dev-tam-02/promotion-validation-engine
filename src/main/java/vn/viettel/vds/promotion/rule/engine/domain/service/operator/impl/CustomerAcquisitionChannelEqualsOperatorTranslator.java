package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Translator for operator {@code customer.acquisition_channel.equals}.
 *
 * <p>Renders a Drools {@code Customer} fact constraint on the {@code acquisitionChannel} field.
 * Supports four comparators via the {@code comparator} param (default: {@code equals}):
 * <ul>
 *   <li>{@code equals}     → {@code acquisitionChannel == "paid"}</li>
 *   <li>{@code not_equals} → {@code acquisitionChannel != "paid"}</li>
 *   <li>{@code in}         → {@code acquisitionChannel in ("paid", "organic")}</li>
 *   <li>{@code not_in}     → {@code acquisitionChannel not in ("paid", "organic")}</li>
 * </ul>
 *
 * <p>For single-value {@code in}/{@code not_in} params, a scalar String is accepted in addition
 * to {@code List<String>} for forward-compatibility.
 */
@Component
public class CustomerAcquisitionChannelEqualsOperatorTranslator implements OperatorTranslator {

    private static final String OPERATOR_NAME = "customer.acquisition_channel.equals";

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
        return OPERATOR_NAME.equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        String comparator = (String) params.getOrDefault("comparator", "equals");
        Object value = params.get("value");

        String constraint = switch (comparator) {
            case "equals" -> renderEquals(value);
            case "not_equals" -> renderNotEquals(value);
            case "in" -> renderIn(toList(value, "in"));
            case "not_in" -> renderNotIn(toList(value, "not_in"));
            default -> throw new IllegalArgumentException(
                    "Unsupported comparator '" + comparator + "' for operator " + OPERATOR_NAME
                            + ". Supported: equals, not_equals, in, not_in");
        };

        return "        $customer: Customer(" + constraint + ")\n";
    }

    // --- constraint renderers ---

    private String renderEquals(Object value) {
        return "acquisitionChannel == \"" + escape(value) + "\"";
    }

    private String renderNotEquals(Object value) {
        return "acquisitionChannel != \"" + escape(value) + "\"";
    }

    private String renderIn(List<String> values) {
        String quoted = values.stream()
                .map(v -> "\"" + escape(v) + "\"")
                .collect(Collectors.joining(", "));
        return "acquisitionChannel in (" + quoted + ")";
    }

    private String renderNotIn(List<String> values) {
        String quoted = values.stream()
                .map(v -> "\"" + escape(v) + "\"")
                .collect(Collectors.joining(", "));
        return "acquisitionChannel not in (" + quoted + ")";
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private List<String> toList(Object value, String comparator) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing required parameter 'value' for comparator '" + comparator
                            + "' on operator " + OPERATOR_NAME);
        }
        if (value instanceof List) {
            List<String> list = (List<String>) value;
            if (list.isEmpty()) {
                throw new IllegalArgumentException(
                        "Parameter 'value' must be non-empty for comparator '" + comparator
                                + "' on operator " + OPERATOR_NAME);
            }
            return list;
        }
        if (value instanceof String s) {
            return List.of(s);
        }
        throw new IllegalArgumentException(
                "Parameter 'value' must be a List<String> or String for comparator '"
                        + comparator + "' on operator " + OPERATOR_NAME);
    }

    private String escape(Object value) {
        if (value == null) return "";
        return value.toString().replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
