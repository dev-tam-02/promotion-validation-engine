package vn.viettel.vds.promotion.rule.engine.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslatorRegistry;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RuleTranslationService {

    private static final Logger logger = LoggerFactory.getLogger(RuleTranslationService.class);
    private static final String CHILDREN_KEY = "children";

    // Node type constants
    private static final String NODE_TYPE_GROUP = "GROUP";
    private static final String NODE_TYPE_COND = "COND";

    // Drools DRL syntax constants
    private static final String DRL_WHEN = "    when\n";
    private static final String DRL_THEN = "    then\n";
    private static final String DRL_END = "end\n\n";
    private final OperatorTranslatorRegistry translatorRegistry;

    public RuleTranslationService(OperatorTranslatorRegistry translatorRegistry) {
        this.translatorRegistry = translatorRegistry;
    }

    private static final String DEFAULT_PACKAGE = "rules";

    public String translateToDrl(List<Map<String, Object>> nodes) {

        // Sort nodes by ID to ensure deterministic DRL generation
        List<Map<String, Object>> sortedNodes = nodes.stream()
                .sorted(Comparator.comparing(node -> (String) node.get("id")))
                .toList();

        // Use LinkedHashMap to preserve insertion order
        Map<String, Map<String, Object>> nodeMap = sortedNodes.stream()
                .collect(Collectors.toMap(
                        node -> (String) node.get("id"),
                        node -> node,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        Map<String, Object> rootNode = findRootNode(nodeMap);
        if (rootNode == null) {
            throw new IllegalArgumentException("No root node found in rule structure");
        }

        StringBuilder drl = new StringBuilder();

        generateDrlHeader(drl);
        generateRule(drl, rootNode, nodeMap);

        return drl.toString();
    }

    private void generateDrlHeader(StringBuilder drl) {
        drl.append("package ").append(DEFAULT_PACKAGE).append(";\n\n");

        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.Customer;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.Order;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.OrderItem;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.Candidate;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.RuleMatched;\n");
        drl.append("import java.util.List;\n");
        drl.append("import java.util.ArrayList;\n");
        drl.append("import java.math.BigDecimal;\n");
        drl.append("import java.time.ZonedDateTime;\n");
        drl.append("import java.time.Instant;\n");
        drl.append("import java.time.ZoneId;\n");
        drl.append("import java.time.DayOfWeek;\n");
        drl.append("import java.time.LocalTime;\n\n");

        drl.append("global ValidationResult result;\n");
        drl.append("global List<String> reasonCodes;\n");
        drl.append("global Object usageService;\n\n");

        // NOTE: checkTimeWindow function is NO LONGER generated here
        // When temporal policy exists, it's generated in timeframe.drl
        // When no temporal policy, time-based operators are not supported
        // This prevents duplicate function definition when both DRLs are in same package
    }

    private void generateRule(StringBuilder drl, Map<String, Object> rootNode,
                              Map<String, Map<String, Object>> nodeMap) {

        // Add debug rule to log input values
        generateDebugRule(drl);

        drl.append("rule \"promotion_validation_rule\"\n");
        drl.append(DRL_WHEN);

        // Require temporal check to pass first (TemporalAllowed is inserted by timeframe.drl)
        drl.append("        TemporalAllowed()\n");

        generateConditions(drl, rootNode, nodeMap, 2);

        drl.append(DRL_THEN);
        drl.append("        System.out.println(\"[DROOLS] ✅ Rule MATCHED - All conditions passed\");\n");
        drl.append("        result.setDecision(\"ALLOW\");\n");
        drl.append("        result.setOk(true);\n");
        drl.append("        insert(new RuleMatched());  // Mark rule as matched to prevent failure rules from firing\n");
        drl.append(DRL_END);

        // Generate negative rules for each condition to track failures
        generateConditionFailureRules(drl, rootNode, nodeMap);

        generateFailureRule(drl);
    }

    private void generateDebugRule(StringBuilder drl) {
        drl.append("rule \"debug_input_values\"\n");
        drl.append("    salience 1000\n");  // High priority to run first
        drl.append(DRL_WHEN);
        drl.append("        $order: Order()\n");
        drl.append("        $customer: Customer()\n");
        drl.append(DRL_THEN);
        drl.append("        System.out.println(\"[DROOLS-DEBUG] ==================== INPUT VALUES ====================\");\n");
        drl.append("        System.out.println(\"[DROOLS-DEBUG] Order.id: \" + $order.getId());\n");
        drl.append("        System.out.println(\"[DROOLS-DEBUG] Order.total: \" + $order.getTotal());\n");
        drl.append("        System.out.println(\"[DROOLS-DEBUG] Order.total type: \" + ($order.getTotal() != null ? $order.getTotal().getClass().getName() : \"null\"));\n");
        drl.append("        System.out.println(\"[DROOLS-DEBUG] Order.currency: \" + $order.getCurrency());\n");
        drl.append("        System.out.println(\"[DROOLS-DEBUG] Order.items.size: \" + ($order.getItems() != null ? $order.getItems().size() : 0));\n");
        drl.append("        System.out.println(\"[DROOLS-DEBUG] Customer.id: \" + $customer.getId());\n");
        drl.append("        System.out.println(\"[DROOLS-DEBUG] Customer.segments: \" + $customer.getSegments());\n");
        drl.append("        System.out.println(\"[DROOLS-DEBUG] Customer.segments type: \" + ($customer.getSegments() != null ? $customer.getSegments().getClass().getName() : \"null\"));\n");
        drl.append("        System.out.println(\"[DROOLS-DEBUG] Customer.segments contains VIP: \" + ($customer.getSegments() != null && $customer.getSegments().contains(\"VIP\")));\n");
        drl.append("        System.out.println(\"[DROOLS-DEBUG] ====================================================\");\n");
        drl.append(DRL_END);
    }

    private void generateConditions(StringBuilder drl, Map<String, Object> node,
                                    Map<String, Map<String, Object>> nodeMap, int indent) {

        String type = (String) node.get("type");

        if (NODE_TYPE_GROUP.equals(type)) {
            generateGroupConditions(drl, node, nodeMap, indent);
        } else if (NODE_TYPE_COND.equals(type)) {
            generateConditionNode(drl, node, indent);
        }
    }

    private void generateGroupConditions(StringBuilder drl, Map<String, Object> groupNode,
                                         Map<String, Map<String, Object>> nodeMap, int indent) {

        String groupLogic = (String) groupNode.get("groupLogic");
        List<String> children = extractChildIds(groupNode);

        if (children.isEmpty()) {
            return;
        }

        String indentStr = " ".repeat(indent);

        switch (groupLogic) {
            case "ALL" -> generateAllConditions(drl, children, nodeMap, indent);
            case "ANY" -> generateAnyConditions(drl, children, nodeMap, indent, indentStr);
            case "NONE" -> generateNoneConditions(drl, children, nodeMap, indent, indentStr);
            default -> logger.warn("Unknown groupLogic value: {}", groupLogic);
        }
    }

    private void generateAllConditions(StringBuilder drl, List<String> children,
                                       Map<String, Map<String, Object>> nodeMap, int indent) {
        // Sort children to ensure deterministic order
        List<String> sortedChildren = new ArrayList<>(children);
        Collections.sort(sortedChildren);

        for (String childId : sortedChildren) {
            Map<String, Object> childNode = nodeMap.get(childId);
            if (childNode != null) {
                generateConditions(drl, childNode, nodeMap, indent);
            }
        }
    }

    private void generateAnyConditions(StringBuilder drl, List<String> children,
                                       Map<String, Map<String, Object>> nodeMap, int indent, String indentStr) {
        // Sort children to ensure deterministic order
        List<String> sortedChildren = new ArrayList<>(children);
        Collections.sort(sortedChildren);

        drl.append(indentStr).append("(\n");
        for (int i = 0; i < sortedChildren.size(); i++) {
            String childId = sortedChildren.get(i);
            Map<String, Object> childNode = nodeMap.get(childId);
            if (childNode != null) {
                if (i > 0) {
                    drl.append(indentStr).append("    or\n");
                }
                generateConditions(drl, childNode, nodeMap, indent + 4);
            }
        }
        drl.append(indentStr).append(")\n");
    }

    private void generateNoneConditions(StringBuilder drl, List<String> children,
                                        Map<String, Map<String, Object>> nodeMap, int indent, String indentStr) {
        // Sort children to ensure deterministic order
        List<String> sortedChildren = new ArrayList<>(children);
        Collections.sort(sortedChildren);

        drl.append(indentStr).append("not (\n");
        for (int i = 0; i < sortedChildren.size(); i++) {
            String childId = sortedChildren.get(i);
            Map<String, Object> childNode = nodeMap.get(childId);
            if (childNode != null) {
                if (i > 0) {
                    drl.append(indentStr).append("    or\n");
                }
                generateConditions(drl, childNode, nodeMap, indent + 4);
            }
        }
        drl.append(indentStr).append(")\n");
    }

    private void generateConditionNode(StringBuilder drl, Map<String, Object> condNode, int indent) {
        String nodeId = (String) condNode.get("id");
        String operatorName = (String) condNode.get("operatorName");
        Integer operatorVersion = (Integer) condNode.get("operatorVersion");
        Map<String, Object> params = (Map<String, Object>) condNode.get("params");
        String reasonCode = (String) condNode.get("reasonCode");

        // Defensive null-check: Initialize empty map if params is null
        if (params == null) {
            params = java.util.Collections.emptyMap();
            logger.debug("No parameters provided for operator '{}' in node '{}', using empty map",
                    operatorName, nodeId);
        }

        OperatorTranslator translator = translatorRegistry.getTranslator(operatorName, operatorVersion);
        String condition = translator.translate(nodeId, params, reasonCode);

        String indentStr = " ".repeat(indent);
        String[] lines = condition.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                drl.append(indentStr).append(line.trim()).append("\n");
            }
        }
    }

    private void generateConditionFailureRules(StringBuilder drl, Map<String, Object> rootNode,
                                               Map<String, Map<String, Object>> nodeMap) {
        // Collect all COND nodes with reason codes
        List<Map<String, Object>> condNodes = collectConditionNodes(rootNode, nodeMap);

        // Sort by node ID for deterministic rule generation
        condNodes.sort(Comparator.comparing(node -> (String) node.get("id")));

        for (Map<String, Object> condNode : condNodes) {
            String nodeId = (String) condNode.get("id");
            String reasonCode = (String) condNode.get("reasonCode");

            if (reasonCode == null || reasonCode.isEmpty()) {
                continue;
            }

            String operatorName = (String) condNode.get("operatorName");
            Integer operatorVersion = (Integer) condNode.get("operatorVersion");
            Map<String, Object> params = (Map<String, Object>) condNode.get("params");

            if (params == null) {
                params = java.util.Collections.emptyMap();
            }

            OperatorTranslator translator = translatorRegistry.getTranslator(operatorName, operatorVersion);
            String condition = translator.translate(nodeId, params, reasonCode);

            // Generate negative rule for this condition
            drl.append("rule \"failure_tracking_").append(sanitizeRuleName(nodeId)).append("\"\n");
            drl.append("    salience -10\n");
            drl.append(DRL_WHEN);
            drl.append("        not RuleMatched()  // Only fire if main rule didn't match\n");
            drl.append("        not (\n");

            // Add the condition (indented)
            String[] lines = condition.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    drl.append("            ").append(line.trim()).append("\n");
                }
            }

            drl.append("        )\n");
            drl.append(DRL_THEN);
            drl.append("        System.out.println(\"[DROOLS] ❌ Condition FAILED - nodeId=")
                    .append(nodeId)
                    .append(", reasonCode=")
                    .append(reasonCode)
                    .append("\");\n");
            drl.append("        reasonCodes.add(\"").append(reasonCode).append("\");\n");
            drl.append(DRL_END);
        }
    }

    private void generateFailureRule(StringBuilder drl) {
        drl.append("rule \"promotion_validation_failure\"\n");
        drl.append("    salience -100\n");
        drl.append(DRL_WHEN);
        drl.append("        not RuleMatched()  // Only fire if main rule didn't match\n");
        drl.append(DRL_THEN);
        drl.append("        System.out.println(\"[DROOLS] 🚫 Overall DENY - reasonCodes=\" + reasonCodes);\n");
        drl.append("        result.setDecision(\"DENY\");\n");
        drl.append("        result.setOk(false);\n");
        drl.append("        result.setReasonCodes(reasonCodes);\n");
        drl.append("end\n");
    }

    private List<Map<String, Object>> collectConditionNodes(Map<String, Object> node,
                                                            Map<String, Map<String, Object>> nodeMap) {
        List<Map<String, Object>> condNodes = new ArrayList<>();
        collectConditionNodesRecursive(node, nodeMap, condNodes);
        return condNodes;
    }

    private void collectConditionNodesRecursive(Map<String, Object> node,
                                                Map<String, Map<String, Object>> nodeMap,
                                                List<Map<String, Object>> condNodes) {
        String type = (String) node.get("type");

        if (NODE_TYPE_COND.equals(type)) {
            condNodes.add(node);
        } else if (NODE_TYPE_GROUP.equals(type)) {
            List<String> children = extractChildIds(node);
            for (String childId : children) {
                Map<String, Object> childNode = nodeMap.get(childId);
                if (childNode != null) {
                    collectConditionNodesRecursive(childNode, nodeMap, condNodes);
                }
            }
        }
    }

    private String sanitizeRuleName(String nodeId) {
        // Replace any non-alphanumeric characters with underscore
        return nodeId.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private Map<String, Object> findRootNode(Map<String, Map<String, Object>> nodeMap) {
        Set<String> childIds = collectAllChildIds(nodeMap);
        return findNodeNotInChildIds(nodeMap, childIds);
    }

    private Set<String> collectAllChildIds(Map<String, Map<String, Object>> nodeMap) {
        Set<String> childIds = new HashSet<>();
        for (Map<String, Object> node : nodeMap.values()) {
            List<String> children = extractChildIds(node);
            childIds.addAll(children);
        }
        return childIds;
    }

    private Map<String, Object> findNodeNotInChildIds(Map<String, Map<String, Object>> nodeMap, Set<String> childIds) {
        return nodeMap.values().stream()
                .filter(node -> !childIds.contains(node.get("id")))
                .findFirst()
                .orElse(null);
    }

    private List<String> extractChildIds(Map<String, Object> node) {
        Object childrenObj = node.get(CHILDREN_KEY);
        if (childrenObj instanceof List) {
            return ((List<?>) childrenObj).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

}