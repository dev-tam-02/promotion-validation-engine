package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Operator translator for product applicability with include/exclude logic.
 *
 * <p>Supports flexible product filtering with the following parameters:</p>
 * <ul>
 *   <li><b>includeAll</b> (Boolean): If true, all products are allowed except those in exclude list.
 *                                     If false, only products in include list are allowed.</li>
 *   <li><b>include</b> (List): List of product IDs that are allowed. Required when includeAll=false.</li>
 *   <li><b>exclude</b> (List): List of product IDs that are denied. Always checked first if present.</li>
 * </ul>
 *
 * <p>Validation Logic (priority order):</p>
 * <ol>
 *   <li>If productId is in exclude list → DENY</li>
 *   <li>If includeAll=true → ALLOW (after passing exclude check)</li>
 *   <li>If includeAll=false:
 *     <ul>
 *       <li>If productId is in include list → ALLOW</li>
 *       <li>If productId is NOT in include list → DENY</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>
 * {
 *   "operatorName": "order.item.product.applicable",
 *   "params": {
 *     "includeAll": false,
 *     "include": ["PROD-001", "PROD-002", "PROD-003"],
 *     "exclude": ["PROD-BAD-001", "PROD-BAD-002"]
 *   },
 *   "reasonCode": "PRODUCT_NOT_APPLICABLE"
 * }
 * </pre>
 *
 * @author Harley Hoang
 * @version 1.0
 * @since 2025-11-14
 */
@Component
public class OrderItemProductApplicableOperatorTranslator implements OperatorTranslator {

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        // Extract parameters
        Boolean includeAllObj = (Boolean) params.get("includeAll");
        Object includeParam = params.get("include");
        Object excludeParam = params.get("exclude");

        // Default includeAll to false if not specified
        boolean includeAll = includeAllObj != null && includeAllObj;

        // Convert to lists
        List<?> includeList = includeParam instanceof List ? (List<?>) includeParam : null;
        List<?> excludeList = excludeParam instanceof List ? (List<?>) excludeParam : null;

        // Validation
        if (!includeAll && (includeList == null || includeList.isEmpty())) {
            throw new IllegalArgumentException(
                    "Parameter 'include' is required and must not be empty when includeAll=false for operator: order.item.product.applicable");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("        exists OrderItem(");

        // Build constraints list
        List<String> constraints = new ArrayList<>();

        // Case 1: includeAll=false → must be in include list
        if (!includeAll) {
            String includeStr = includeList.stream()
                    .map(id -> "\"" + id + "\"")
                    .collect(Collectors.joining(", "));
            constraints.add("productId in (" + includeStr + ")");
        }

        // Case 2: exclude list → must NOT be in exclude list
        if (excludeList != null && !excludeList.isEmpty()) {
            String excludeStr = excludeList.stream()
                    .map(id -> "\"" + id + "\"")
                    .collect(Collectors.joining(", "));
            constraints.add("productId not in (" + excludeStr + ")");
        }

        // Join constraints with comma (AND in Drools)
        if (!constraints.isEmpty()) {
            sb.append(String.join(", ", constraints));
        }

        sb.append(")\n");

        return sb.toString();
    }

    @Override
    public String getOperatorName() {
        return "order.item.product.applicable";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "order.item.product.applicable".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}
