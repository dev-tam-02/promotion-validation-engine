package vn.viettel.vds.promotion.validation.engine.domain.service;

import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslatorRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RuleTranslationService {

    private final OperatorTranslatorRegistry translatorRegistry;

    public RuleTranslationService(OperatorTranslatorRegistry translatorRegistry) {
        this.translatorRegistry = translatorRegistry;
    }

    public String translateToDrl(String tenantId, String ruleId, Integer version,
                                 List<Map<String, Object>> nodes) {

        Map<String, Map<String, Object>> nodeMap = nodes.stream()
                .collect(Collectors.toMap(
                        node -> (String) node.get("id"),
                        node -> node
                ));

        Map<String, Object> rootNode = findRootNode(nodeMap);
        if (rootNode == null) {
            throw new IllegalArgumentException("No root node found in rule structure");
        }

        StringBuilder drl = new StringBuilder();

        generateDrlHeader(drl, tenantId, ruleId, version);
        generateRule(drl, ruleId, rootNode, nodeMap);

        return drl.toString();
    }

    private void generateDrlHeader(StringBuilder drl, String tenantId, String ruleId, Integer version) {
        // Convert tenantId to valid package name - replace "default" and sanitize
        String packageName = sanitizePackageName(tenantId);
        drl.append("package ").append(packageName).append(";\n\n");

        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.Customer;\n");
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.Order;\n");
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.OrderItem;\n");
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.Candidate;\n");
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;\n");
        drl.append("import java.util.List;\n");
        drl.append("import java.util.ArrayList;\n\n");

        drl.append("global ValidationResult result;\n");
        drl.append("global List<String> reasonCodes;\n");
        drl.append("global Object timeWindowService;\n");
        drl.append("global Object usageService;\n\n");
    }

    private void generateRule(StringBuilder drl, String ruleId, Map<String, Object> rootNode,
                              Map<String, Map<String, Object>> nodeMap) {

        drl.append("rule \"").append(ruleId).append("\"\n");
        drl.append("    when\n");

        generateConditions(drl, rootNode, nodeMap, 2);

        drl.append("    then\n");
        drl.append("        result.setDecision(\"ALLOW\");\n");
        drl.append("        result.setOk(true);\n");
        drl.append("end\n\n");

        generateFailureRule(drl, ruleId, rootNode, nodeMap);
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
        List<String> children = (List<String>) groupNode.get("children");

        if (children == null || children.isEmpty()) {
            return;
        }

        String indentStr = " ".repeat(indent);

        if ("ALL".equals(groupLogic)) {
            for (String childId : children) {
                Map<String, Object> childNode = nodeMap.get(childId);
                if (childNode != null) {
                    generateConditions(drl, childNode, nodeMap, indent);
                }
            }
        } else if ("ANY".equals(groupLogic)) {
            drl.append(indentStr).append("(\n");
            for (int i = 0; i < children.size(); i++) {
                String childId = children.get(i);
                Map<String, Object> childNode = nodeMap.get(childId);
                if (childNode != null) {
                    if (i > 0) {
                        drl.append(indentStr).append("    or\n");
                    }
                    generateConditions(drl, childNode, nodeMap, indent + 4);
                }
            }
            drl.append(indentStr).append(")\n");
        } else if ("NONE".equals(groupLogic)) {
            drl.append(indentStr).append("not (\n");
            for (int i = 0; i < children.size(); i++) {
                String childId = children.get(i);
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
    }

    private void generateConditionNode(StringBuilder drl, Map<String, Object> condNode, int indent) {
        String nodeId = (String) condNode.get("id");
        String operatorName = (String) condNode.get("operatorName");
        Integer operatorVersion = (Integer) condNode.get("operatorVersion");
        Map<String, Object> params = (Map<String, Object>) condNode.get("params");
        String reasonCode = (String) condNode.get("reasonCode");

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

    private void generateFailureRule(StringBuilder drl, String ruleId, Map<String, Object> rootNode,
                                     Map<String, Map<String, Object>> nodeMap) {

        Set<String> allReasonCodes = collectReasonCodes(rootNode, nodeMap);

        drl.append("rule \"").append(ruleId).append("_failure\"\n");
        drl.append("    salience -100\n");
        drl.append("    when\n");
        drl.append("        not ValidationResult(decision == \"ALLOW\")\n");
        drl.append("    then\n");
        drl.append("        result.setDecision(\"DENY\");\n");
        drl.append("        result.setOk(false);\n");

        if (!allReasonCodes.isEmpty()) {
            drl.append("        reasonCodes.addAll(java.util.Arrays.asList(");
            String codes = allReasonCodes.stream()
                    .map(code -> "\"" + code + "\"")
                    .collect(Collectors.joining(", "));
            drl.append(codes);
            drl.append("));\n");
        }

        drl.append("        result.setReasonCodes(reasonCodes);\n");
        drl.append("end\n");
    }

    private Set<String> collectReasonCodes(Map<String, Object> node, Map<String, Map<String, Object>> nodeMap) {
        Set<String> reasonCodes = new HashSet<>();
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
            List<String> children = (List<String>) node.get("children");
            if (children != null) {
                for (String childId : children) {
                    Map<String, Object> childNode = nodeMap.get(childId);
                    if (childNode != null) {
                        collectReasonCodesRecursive(childNode, nodeMap, reasonCodes);
                    }
                }
            }
        }
    }

    private Map<String, Object> findRootNode(Map<String, Map<String, Object>> nodeMap) {
        Set<String> childIds = new HashSet<>();

        for (Map<String, Object> node : nodeMap.values()) {
            List<String> children = (List<String>) node.get("children");
            if (children != null) {
                childIds.addAll(children);
            }
        }

        return nodeMap.values().stream()
                .filter(node -> !childIds.contains(node.get("id")))
                .findFirst()
                .orElse(null);
    }

    private String sanitizePackageName(String tenantId) {
        // Handle reserved Java keywords and invalid package names
        if (tenantId == null || tenantId.isEmpty() || "default".equalsIgnoreCase(tenantId)) {
            return "tenant.defaulttenant";
        }

        // Convert to lowercase and replace invalid characters
        String sanitized = tenantId.toLowerCase()
                .replaceAll("[^a-z0-9_.]", "_")
                .replaceAll("^[0-9]", "_$0"); // Prefix with _ if starts with number

        // Ensure it doesn't start with a reserved word
        if (isReservedKeyword(sanitized)) {
            sanitized = "tenant." + sanitized;
        }

        return sanitized;
    }

    private boolean isReservedKeyword(String word) {
        // Java reserved keywords
        String[] keywords = {"abstract", "assert", "boolean", "break", "byte",
                "case", "catch", "char", "class", "const", "continue", "default",
                "do", "double", "else", "enum", "extends", "final", "finally",
                "float", "for", "goto", "if", "implements", "import", "instanceof",
                "int", "interface", "long", "native", "new", "package", "private",
                "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws",
                "transient", "try", "void", "volatile", "while"};

        for (String keyword : keywords) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }
}