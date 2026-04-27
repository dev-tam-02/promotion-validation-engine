package vn.viettel.vds.promotion.rule.engine.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslatorRegistry;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    // Regex patterns for variable binding deduplication in DRL generation
    private static final Pattern BARE_BINDING_PATTERN = Pattern.compile("^(\\$\\w+):\\s*\\w+\\(\\)$");
    private static final Pattern VAR_BINDING_PATTERN = Pattern.compile("^(\\$\\w+):\\s*\\w+\\(");

    private final OperatorTranslatorRegistry translatorRegistry;

    public RuleTranslationService(OperatorTranslatorRegistry translatorRegistry) {
        this.translatorRegistry = translatorRegistry;
    }

    private static final String DEFAULT_PACKAGE = "rules";

    public String translateToDrl(List<Map<String, Object>> nodes) {
        return translateToDrl(nodes, false);
    }

    public String translateToDrl(List<Map<String, Object>> nodes, boolean hasTemporalPolicy) {

        // Sort nodes by ID to ensure deterministic DRL generation
        List<Map<String, Object>> sortedNodes = nodes.stream()
                .sorted(Comparator.comparing(node -> {
                    String id = (String) node.get("id");
                    return id != null ? id : "";
                }))
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

        generateDrlHeader(drl, hasTemporalPolicy);
        generateRule(drl, rootNode, nodeMap, hasTemporalPolicy);

        return drl.toString();
    }

    private void generateDrlHeader(StringBuilder drl, boolean hasTemporalPolicy) {
        drl.append("package ").append(DEFAULT_PACKAGE).append(";\n\n");

        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.Customer;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.CustomerFact;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.Order;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.OrderItem;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.Candidate;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.RuleMatched;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.LimitsCtx;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.VoucherFact;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.QuotaPolicy;\n");
        drl.append("import vn.viettel.vds.promotion.rule.engine.application.service.BucketKeys;\n");
        drl.append("import java.util.List;\n");
        drl.append("import java.util.ArrayList;\n");
        drl.append("import java.math.BigDecimal;\n");
        drl.append("import java.time.ZonedDateTime;\n");
        drl.append("import java.time.Instant;\n");
        drl.append("import java.time.ZoneId;\n");
        drl.append("import java.time.DayOfWeek;\n");
        drl.append("import java.time.LocalTime;\n\n");

        drl.append("global ValidationResult result;\n");
        drl.append("global List<String> reasonCodes;\n\n");

        // When no temporal policy, we need to declare TemporalAllowed locally
        // and insert it automatically so the rule can still match
        if (!hasTemporalPolicy) {
            drl.append("// No temporal policy - declare TemporalAllowed locally\n");
            drl.append("declare TemporalAllowed\n");
            drl.append(DRL_END);
        }
    }

    private void generateRule(StringBuilder drl, Map<String, Object> rootNode,
                              Map<String, Map<String, Object>> nodeMap, boolean hasTemporalPolicy) {

        // When no temporal policy, add rule to insert TemporalAllowed automatically
        if (!hasTemporalPolicy) {
            drl.append("rule \"insert_temporal_allowed\"\n");
            drl.append("    salience 9999\n");  // Very high priority to run first
            drl.append(DRL_WHEN);
            drl.append("        not TemporalAllowed()\n");
            drl.append(DRL_THEN);
            drl.append("        insert(new TemporalAllowed());\n");
            drl.append(DRL_END);
        }

        drl.append("rule \"promotion_validation_rule\"\n");
        drl.append(DRL_WHEN);

        // Require temporal check to pass first (TemporalAllowed is inserted by timeframe.drl or by insert_temporal_allowed rule)
        drl.append("        TemporalAllowed()\n");

        Set<String> boundVars = new HashSet<>();
        List<String> counterPolicyStatements = new ArrayList<>();
        generateConditions(drl, rootNode, nodeMap, 2, boundVars, counterPolicyStatements);

        drl.append(DRL_THEN);
        // Emit counter-policy addPolicy statements before the ALLOW verdict
        for (String stmt : counterPolicyStatements) {
            drl.append("        ").append(stmt).append("\n");
        }
        drl.append("        result.setDecision(\"ALLOW\");\n");
        drl.append("        result.setOk(true);\n");
        drl.append("        insert(new RuleMatched());  // Mark rule as matched to prevent failure rules from firing\n");
        drl.append(DRL_END);

        // Generate negative rules for each condition to track failures
        generateConditionFailureRules(drl, rootNode, nodeMap);

        generateFailureRule(drl);
    }

    private void generateConditions(StringBuilder drl, Map<String, Object> node,
                                    Map<String, Map<String, Object>> nodeMap, int indent,
                                    Set<String> boundVars) {
        generateConditions(drl, node, nodeMap, indent, boundVars, null);
    }

    private void generateConditions(StringBuilder drl, Map<String, Object> node,
                                    Map<String, Map<String, Object>> nodeMap, int indent,
                                    Set<String> boundVars, List<String> counterPolicyStatements) {

        String type = (String) node.get("type");

        if (NODE_TYPE_GROUP.equals(type)) {
            generateGroupConditions(drl, node, nodeMap, indent, boundVars, counterPolicyStatements);
        } else if (NODE_TYPE_COND.equals(type)) {
            generateConditionNode(drl, node, indent, boundVars, counterPolicyStatements);
        }
    }

    private void generateGroupConditions(StringBuilder drl, Map<String, Object> groupNode,
                                         Map<String, Map<String, Object>> nodeMap, int indent,
                                         Set<String> boundVars, List<String> counterPolicyStatements) {

        String groupLogic = (String) groupNode.get("groupLogic");
        List<String> children = extractChildIds(groupNode);

        if (children.isEmpty()) {
            return;
        }

        String indentStr = " ".repeat(indent);

        switch (groupLogic) {
            case "ALL" -> generateAllConditions(drl, children, nodeMap, indent, boundVars, counterPolicyStatements);
            case "ANY" -> generateAnyConditions(drl, children, nodeMap, indent, indentStr, boundVars, counterPolicyStatements);
            case "NONE" -> generateNoneConditions(drl, children, nodeMap, indent, indentStr, boundVars, counterPolicyStatements);
            case "XOR" -> generateXorConditions(drl, children, nodeMap, indent, indentStr, boundVars, counterPolicyStatements);
            default -> logger.warn("Unknown groupLogic value: {}", groupLogic);
        }
    }

    private void generateAllConditions(StringBuilder drl, List<String> children,
                                       Map<String, Map<String, Object>> nodeMap, int indent,
                                       Set<String> boundVars, List<String> counterPolicyStatements) {
        // Sort children to ensure deterministic order
        List<String> sortedChildren = new ArrayList<>(children);
        Collections.sort(sortedChildren);

        for (String childId : sortedChildren) {
            Map<String, Object> childNode = nodeMap.get(childId);
            if (childNode != null) {
                generateConditions(drl, childNode, nodeMap, indent, boundVars, counterPolicyStatements);
            }
        }
    }

    private void generateAnyConditions(StringBuilder drl, List<String> children,
                                       Map<String, Map<String, Object>> nodeMap, int indent, String indentStr,
                                       Set<String> boundVars, List<String> counterPolicyStatements) {
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
                generateConditions(drl, childNode, nodeMap, indent + 4, boundVars, counterPolicyStatements);
            }
        }
        drl.append(indentStr).append(")\n");
    }

    private void generateNoneConditions(StringBuilder drl, List<String> children,
                                        Map<String, Map<String, Object>> nodeMap, int indent, String indentStr,
                                        Set<String> boundVars, List<String> counterPolicyStatements) {
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
                generateConditions(drl, childNode, nodeMap, indent + 4, boundVars, counterPolicyStatements);
            }
        }
        drl.append(indentStr).append(")\n");
    }

    /**
     * XOR logic: exactly one child condition must be true.
     * Implemented as: (A and not B and not C) or (not A and B and not C) or (not A and not B and C)
     * Simplified: ANY of children matches, but not more than one.
     * Drools approach: use ANY + eval guard that counts matches.
     */
    private void generateXorConditions(StringBuilder drl, List<String> children,
                                        Map<String, Map<String, Object>> nodeMap, int indent, String indentStr,
                                        Set<String> boundVars, List<String> counterPolicyStatements) {
        List<String> sortedChildren = new ArrayList<>(children);
        Collections.sort(sortedChildren);

        // XOR = exactly one must match. Generate: for each child, that child matches AND all others don't.
        drl.append(indentStr).append("(\n");
        for (int i = 0; i < sortedChildren.size(); i++) {
            if (i > 0) {
                drl.append(indentStr).append("    or\n");
            }
            drl.append(indentStr).append("    (\n");
            for (int j = 0; j < sortedChildren.size(); j++) {
                Map<String, Object> childNode = nodeMap.get(sortedChildren.get(j));
                if (childNode == null) continue;

                if (j == i) {
                    // This child MUST match
                    generateConditions(drl, childNode, nodeMap, indent + 8, boundVars, counterPolicyStatements);
                } else {
                    // Other children must NOT match
                    drl.append(indentStr).append("        not (\n");
                    generateConditions(drl, childNode, nodeMap, indent + 12, boundVars, counterPolicyStatements);
                    drl.append(indentStr).append("        )\n");
                }
            }
            drl.append(indentStr).append("    )\n");
        }
        drl.append(indentStr).append(")\n");
    }

    @SuppressWarnings("unchecked")
    private void generateConditionNode(StringBuilder drl, Map<String, Object> condNode, int indent,
                                       Set<String> boundVars, List<String> counterPolicyStatements) {
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

        // Counter-policy translators: fact pattern goes to when; then-statement collected separately
        if (translator.isCounterPolicy() && counterPolicyStatements != null) {
            String factPattern = translator.getCounterFactPattern();
            if (factPattern != null && !factPattern.isBlank()) {
                String trimmed = factPattern.trim();
                Matcher bareMatcher = BARE_BINDING_PATTERN.matcher(trimmed);
                if (bareMatcher.matches()) {
                    String varName = bareMatcher.group(1);
                    if (!boundVars.contains(varName)) {
                        boundVars.add(varName);
                        drl.append(" ".repeat(indent)).append(trimmed).append("\n");
                    }
                } else {
                    Matcher varMatcher = VAR_BINDING_PATTERN.matcher(trimmed);
                    if (varMatcher.find()) {
                        String varName = varMatcher.group(1);
                        if (!boundVars.contains(varName)) {
                            boundVars.add(varName);
                            drl.append(" ".repeat(indent)).append(trimmed).append("\n");
                        }
                    } else {
                        drl.append(" ".repeat(indent)).append(trimmed).append("\n");
                    }
                }
            }
            // Collect the then-clause statement
            String thenStmt = translator.translate(nodeId, params, reasonCode);
            if (thenStmt != null && !thenStmt.isBlank()) {
                counterPolicyStatements.add(thenStmt.trim());
            }
            return;
        }

        String condition = translator.translate(nodeId, params, reasonCode);

        String indentStr = " ".repeat(indent);
        String[] lines = condition.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Deduplicate variable bindings: skip bare bindings ($var: Type()) if variable is already bound
            Matcher bareMatcher = BARE_BINDING_PATTERN.matcher(trimmed);
            if (bareMatcher.matches()) {
                String varName = bareMatcher.group(1);
                if (boundVars.contains(varName)) {
                    continue;
                }
                boundVars.add(varName);
            } else {
                Matcher varMatcher = VAR_BINDING_PATTERN.matcher(trimmed);
                if (varMatcher.find()) {
                    boundVars.add(varMatcher.group(1));
                }
            }

            drl.append(indentStr).append(trimmed).append("\n");
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

            // Counter-policy translators emit RHS statements — they do not have a when-clause condition to negate
            if (translator.isCounterPolicy()) {
                continue;
            }

            String condition = translator.translate(nodeId, params, reasonCode);

            // Generate negative rule for this condition
            drl.append("rule \"failure_tracking_").append(sanitizeRuleName(nodeId)).append("\"\n");
            drl.append("    salience -10\n");
            drl.append(DRL_WHEN);
            drl.append("        not RuleMatched()  // Only fire if main rule didn't match\n");

            // Split condition: extract variable bindings (e.g. $order: Order()) outside not(),
            // and the actual constraint inside not()
            String[] lines = condition.split("\n");
            List<String> bindingLines = new ArrayList<>();
            List<String> constraintLines = new ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                // Variable binding lines like "$order: Order()" go BEFORE not()
                if (trimmed.matches("\\$\\w+:\\s*\\w+\\(\\)")) {
                    bindingLines.add(trimmed);
                } else {
                    constraintLines.add(trimmed);
                }
            }

            for (String binding : bindingLines) {
                drl.append("        ").append(binding).append("\n");
            }
            drl.append("        not (\n");
            for (String constraintLine : constraintLines) {
                drl.append("            ").append(constraintLine).append("\n");
            }
            drl.append("        )\n");
            drl.append(DRL_THEN);
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