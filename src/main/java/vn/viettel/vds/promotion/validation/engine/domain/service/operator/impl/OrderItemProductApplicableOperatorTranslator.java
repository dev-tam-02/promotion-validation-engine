package vn.viettel.vds.promotion.validation.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;

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
        Boolean includeAll = (Boolean) params.get("includeAll");
        Object includeParam = params.get("include");
        Object excludeParam = params.get("exclude");

        // Default includeAll to false if not specified
        if (includeAll == null) {
            includeAll = false;
        }

        // Convert to lists
        List<?> includeList = includeParam instanceof List ? (List<?>) includeParam : null;
        List<?> excludeList = excludeParam instanceof List ? (List<?>) excludeParam : null;

        // Validation
        if (!includeAll && (includeList == null || includeList.isEmpty())) {
            throw new IllegalArgumentException(
                "Parameter 'include' is required and must not be empty when includeAll=false for operator: order.item.product.applicable");
        }

        StringBuilder sb = new StringBuilder();

        // 1. Exclude check - always processed first if present
        // Generate: not exists OrderItem(productId in ("PROD-BAD-1", "PROD-BAD-2"))
        if (excludeList != null && !excludeList.isEmpty()) {
            String excludeStr = excludeList.stream()
                    .map(id -> "\"" + id + "\"")
                    .collect(Collectors.joining(", "));
            sb.append("        not exists OrderItem(productId in (")
              .append(excludeStr)
              .append("))\n");
        }

        // 2. Include check - only if includeAll=false
        // Generate: exists OrderItem(productId in ("PROD-1", "PROD-2", "PROD-3"))
        if (!includeAll) {
            String includeStr = includeList.stream()
                    .map(id -> "\"" + id + "\"")
                    .collect(Collectors.joining(", "));
            sb.append("        exists OrderItem(productId in (")
              .append(includeStr)
              .append("))\n");
        }

        // If includeAll=true and no exclude list, we need at least one condition
        // In this case, we just check that OrderItem exists (allow any product)
        if (includeAll && (excludeList == null || excludeList.isEmpty())) {
            sb.append("        exists OrderItem()\n");
        }

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
