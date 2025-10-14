package vn.viettel.vds.promotion.validation.engine.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslatorRegistry;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RuleTranslationService {

    private static final Logger logger = LoggerFactory.getLogger(RuleTranslationService.class);
    private static final String CHILDREN_KEY = "children";

    private final OperatorTranslatorRegistry translatorRegistry;

    public RuleTranslationService(OperatorTranslatorRegistry translatorRegistry) {
        this.translatorRegistry = translatorRegistry;
    }

    public String translateToDrl(String tenantId, List<Map<String, Object>> nodes) {

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

        generateDrlHeader(drl, tenantId);
        generateRule(drl, rootNode, nodeMap);

        return drl.toString();
    }

    private void generateDrlHeader(StringBuilder drl, String tenantId) {
        // Convert tenantId to valid package name - replace "default" and sanitize
        String packageName = sanitizePackageName(tenantId);
        drl.append("package ").append(packageName).append(";\n\n");

        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.Customer;\n");
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.Order;\n");
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.OrderItem;\n");
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.Candidate;\n");
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;\n");
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.service.TimeWindowService;\n");
        drl.append("import java.util.List;\n");
        drl.append("import java.util.ArrayList;\n\n");

        drl.append("global ValidationResult result;\n");
        drl.append("global List<String> reasonCodes;\n");
        drl.append("global TimeWindowService timeWindowService;\n");
        drl.append("global Object usageService;\n\n");
    }

    private void generateRule(StringBuilder drl, Map<String, Object> rootNode,
                              Map<String, Map<String, Object>> nodeMap) {

        drl.append("rule \"promotion_validation_rule\"\n");
        drl.append("    when\n");

        generateConditions(drl, rootNode, nodeMap, 2);

        drl.append("    then\n");
        drl.append("        result.setDecision(\"ALLOW\");\n");
        drl.append("        result.setOk(true);\n");
        drl.append("end\n\n");

        generateFailureRule(drl, rootNode, nodeMap);
    }

    private void generateConditions(StringBuilder drl, Map<String, Object> node,
                                    Map<String, Map<String, Object>> nodeMap, int indent) {

        String type = (String) node.get("type");

        if ("GROUP".equals(type)) {
            generateGroupConditions(drl, node, nodeMap, indent);
        } else if ("COND".equals(type)) {
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

    private void generateFailureRule(StringBuilder drl, Map<String, Object> rootNode,
                                     Map<String, Map<String, Object>> nodeMap) {

        Set<String> allReasonCodes = collectReasonCodes(rootNode, nodeMap);

        drl.append("rule \"promotion_validation_failure\"\n");
        drl.append("    salience -100\n");
        drl.append("    when\n");
        drl.append("        not ValidationResult(decision == \"ALLOW\")\n");
        drl.append("    then\n");
        drl.append("        result.setDecision(\"DENY\");\n");
        drl.append("        result.setOk(false);\n");

        if (!allReasonCodes.isEmpty()) {
            drl.append("        reasonCodes.addAll(java.util.Arrays.asList(");
            // Sort reason codes alphabetically to ensure deterministic order
            String codes = allReasonCodes.stream()
                    .sorted()
                    .map(code -> "\"" + code + "\"")
                    .collect(Collectors.joining(", "));
            drl.append(codes);
            drl.append("));\n");
        }

        drl.append("        result.setReasonCodes(reasonCodes);\n");
        drl.append("end\n");
    }

    private Set<String> collectReasonCodes(Map<String, Object> node, Map<String, Map<String, Object>> nodeMap) {
        // Use LinkedHashSet to preserve insertion order
        Set<String> reasonCodes = new LinkedHashSet<>();
        collectReasonCodesRecursive(node, nodeMap, reasonCodes);
        return reasonCodes;
    }

    private void collectReasonCodesRecursive(Map<String, Object> node, Map<String, Map<String, Object>> nodeMap,
                                             Set<String> reasonCodes) {
        String type = (String) node.get("type");

        if ("COND".equals(type)) {
            String reasonCode = (String) node.get("reasonCode");
            if (reasonCode != null) {
                reasonCodes.add(reasonCode);
            }
        } else if ("GROUP".equals(type)) {
            List<String> children = extractChildIds(node);
            if (!children.isEmpty()) {
                // Sort children to ensure deterministic collection order
                List<String> sortedChildren = new ArrayList<>(children);
                Collections.sort(sortedChildren);

                for (String childId : sortedChildren) {
                    Map<String, Object> childNode = nodeMap.get(childId);
                    if (childNode != null) {
                        collectReasonCodesRecursive(childNode, nodeMap, reasonCodes);
                    }
                }
            }
        }
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

    private String sanitizePackageName(String tenantId) {
        // Handle reserved Java keywords and invalid package names
        if (tenantId == null || tenantId.isEmpty() || "default".equalsIgnoreCase(tenantId)) {
            return "tenant.defaulttenant";
        }

        // Convert to lowercase and replace invalid characters
        String sanitized = tenantId.toLowerCase()
                .replaceAll("[^a-z0-9_.]", "_")
                .replaceAll("^\\\\d", "_$0"); // Prefix with _ if starts with number

        // Ensure it doesn't start with a reserved word
        if (isReservedKeyword(sanitized)) {
            sanitized = "tenant." + sanitized;
        }

        return sanitized;
    }

    private static final Set<String> JAVA_RESERVED_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const", "continue", "default",
            "do", "double", "else", "enum", "extends", "final", "finally",
            "float", "for", "goto", "if", "implements", "import", "instanceof",
            "int", "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    );

    private boolean isReservedKeyword(String word) {
        return JAVA_RESERVED_KEYWORDS.contains(word);
    }
}