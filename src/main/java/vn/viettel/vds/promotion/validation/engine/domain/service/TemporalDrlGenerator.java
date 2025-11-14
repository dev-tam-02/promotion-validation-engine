package vn.viettel.vds.promotion.validation.engine.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileRequest.TemporalPolicyData;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileRequest.TimeWindow;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service to generate temporal DRL (timeframe.drl) from temporal policy data.
 *
 * Responsible for:
 * - Generating Drools DRL syntax for temporal validation
 * - Converting RFC 5545 RRULE to Drools conditions
 * - Creating time window checking logic
 *
 * Generated DRL structure:
 * 1. checkTimeWindow() function for time validation
 * 2. temporal_check_allow rule (salience 1000) - checks if current time is within allowed windows
 * 3. temporal_check_deny rule (salience 999) - denies if temporal check failed
 */
@Service
public class TemporalDrlGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TemporalDrlGenerator.class);
    private static final Pattern BYDAY_PATTERN = Pattern.compile("BYDAY=([A-Z,]+)");

    /**
     * Generate timeframe.drl from temporal policy data.
     *
     * @param tenantId tenant identifier (for package name - to match validation DRL package)
     * @param assignmentId assignment identifier (for logging)
     * @param temporalData temporal policy data
     * @return DRL string content
     */
    public String generateTimeframeDrl(String tenantId, String assignmentId, TemporalPolicyData temporalData) {
        logger.info("Generating timeframe.drl for tenantId={}, assignmentId={}", tenantId, assignmentId);

        if (temporalData == null) {
            logger.warn("No temporal data provided for assignmentId={}, generating empty temporal DRL", assignmentId);
            return generateEmptyTemporalDrl(tenantId, assignmentId);
        }

        // Extract data
        String timezone = temporalData.getTimezone() != null ? temporalData.getTimezone() : "UTC";
        String daysOfWeekCsv = extractDaysOfWeekFromRRule(temporalData.getRrule());
        List<TimeWindow> windows = temporalData.getWindows();

        logger.debug("Generating DRL: tz={}, rrule={}, daysOfWeek={}, windowsCount={}",
                timezone, temporalData.getRrule(), daysOfWeekCsv, windows != null ? windows.size() : 0);

        // Generate DRL content
        StringBuilder drl = new StringBuilder();
        generateHeader(drl, tenantId);
        generateCheckTimeWindowFunction(drl);
        generateTemporalRules(drl, windows, timezone, daysOfWeekCsv);

        String result = drl.toString();
        logger.info("Generated timeframe.drl for tenantId={}, assignmentId={}, size={} bytes",
                tenantId, assignmentId, result.length());
        return result;
    }

    /**
     * Generate empty temporal DRL (always allow) when no temporal constraints.
     */
    private String generateEmptyTemporalDrl(String tenantId, String assignmentId) {
        StringBuilder drl = new StringBuilder();
        generateHeader(drl, tenantId);

        // Always allow rule (no temporal constraints)
        drl.append("rule \"temporal_always_allow\"\n");
        drl.append("    salience 1000\n");
        drl.append("    when\n");
        drl.append("        // No temporal constraints - always allow\n");
        drl.append("    then\n");
        drl.append("        insert(new TemporalAllowed());\n");
        drl.append("        System.out.println(\"[TEMPORAL] ✅ No temporal constraints - ALLOWED\");\n");
        drl.append("end\n\n");

        return drl.toString();
    }

    private void generateHeader(StringBuilder drl, String tenantId) {
        // Use tenant package name to match validation-rule.drl package
        // This ensures TemporalAllowed fact type can be shared between DRLs
        String packageName = sanitizePackageName(tenantId);

        drl.append("package ").append(packageName).append(";\n\n");

        // Imports
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;\n");
        drl.append("import java.time.ZonedDateTime;\n");
        drl.append("import java.time.Instant;\n");
        drl.append("import java.time.ZoneId;\n");
        drl.append("import java.time.DayOfWeek;\n");
        drl.append("import java.time.LocalTime;\n");
        drl.append("import java.util.List;\n\n");

        // Global
        drl.append("global ValidationResult result;\n");
        drl.append("global List<String> reasonCodes;\n\n");

        // Fact class for temporal state
        drl.append("declare TemporalAllowed\n");
        drl.append("end\n\n");
    }

    private void generateCheckTimeWindowFunction(StringBuilder drl) {
        drl.append("function boolean checkTimeWindow(String startTime, String endTime, String timezone, boolean spansMidnight, String daysOfWeekCsv) {\n");
        drl.append("    ZonedDateTime zdt = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.of(timezone));\n");
        drl.append("    DayOfWeek currentDay = zdt.getDayOfWeek();\n");
        drl.append("    LocalTime currentTime = zdt.toLocalTime();\n");
        drl.append("    LocalTime start = LocalTime.parse(startTime);\n");
        drl.append("    LocalTime end = LocalTime.parse(endTime);\n");
        drl.append("    \n");
        drl.append("    // Check day of week if specified\n");
        drl.append("    if (daysOfWeekCsv != null && !daysOfWeekCsv.isEmpty()) {\n");
        drl.append("        boolean dayMatches = false;\n");
        drl.append("        String[] daysOfWeek = daysOfWeekCsv.split(\",\");\n");
        drl.append("        for (String day : daysOfWeek) {\n");
        drl.append("            if (currentDay.name().equals(day.trim())) {\n");
        drl.append("                dayMatches = true;\n");
        drl.append("                break;\n");
        drl.append("            }\n");
        drl.append("        }\n");
        drl.append("        if (!dayMatches) return false;\n");
        drl.append("    }\n");
        drl.append("    \n");
        drl.append("    // Check time range\n");
        drl.append("    if (spansMidnight) {\n");
        drl.append("        return currentTime.isAfter(start) || currentTime.equals(start) || currentTime.isBefore(end) || currentTime.equals(end);\n");
        drl.append("    } else {\n");
        drl.append("        return (currentTime.isAfter(start) || currentTime.equals(start)) && (currentTime.isBefore(end) || currentTime.equals(end));\n");
        drl.append("    }\n");
        drl.append("}\n\n");
    }

    private void generateTemporalRules(StringBuilder drl, List<TimeWindow> windows,
                                       String timezone, String daysOfWeekCsv) {

        if (windows == null || windows.isEmpty()) {
            logger.warn("No time windows defined in temporal policy, generating 24/7 allow rule");
            generateAlwaysAllowRule(drl);
            return;
        }

        // Generate allow rule with time window checks
        drl.append("rule \"temporal_check_allow\"\n");
        drl.append("    salience 1000\n");
        drl.append("    when\n");

        // Generate conditions for each window (OR logic)
        if (windows.size() == 1) {
            TimeWindow window = windows.get(0);
            boolean spansMidnight = isSpansMidnight(window.getStartTime(), window.getEndTime());
            drl.append(String.format("        eval(checkTimeWindow(\"%s\", \"%s\", \"%s\", %s, \"%s\"))\n",
                    window.getStartTime(), window.getEndTime(), timezone, spansMidnight, daysOfWeekCsv));
        } else {
            drl.append("        (\n");
            for (int i = 0; i < windows.size(); i++) {
                TimeWindow window = windows.get(i);
                boolean spansMidnight = isSpansMidnight(window.getStartTime(), window.getEndTime());

                if (i > 0) {
                    drl.append("            or\n");
                }
                drl.append(String.format("            eval(checkTimeWindow(\"%s\", \"%s\", \"%s\", %s, \"%s\"))\n",
                        window.getStartTime(), window.getEndTime(), timezone, spansMidnight, daysOfWeekCsv));
            }
            drl.append("        )\n");
        }

        drl.append("    then\n");
        drl.append("        insert(new TemporalAllowed());\n");
        drl.append(String.format("        System.out.println(\"[TEMPORAL] ✅ Time window ACTIVE - tz=%s, days=%s\");\n",
                timezone, daysOfWeekCsv));
        drl.append("end\n\n");

        // Generate deny rule (fallback when temporal check fails)
        generateDenyRule(drl);
    }

    private void generateAlwaysAllowRule(StringBuilder drl) {
        drl.append("rule \"temporal_check_allow_24_7\"\n");
        drl.append("    salience 1000\n");
        drl.append("    when\n");
        drl.append("        // No time windows defined - allow 24/7\n");
        drl.append("    then\n");
        drl.append("        insert(new TemporalAllowed());\n");
        drl.append("        System.out.println(\"[TEMPORAL] ✅ 24/7 ACTIVE (no time restrictions)\");\n");
        drl.append("end\n\n");
    }

    private void generateDenyRule(StringBuilder drl) {
        drl.append("rule \"temporal_check_deny\"\n");
        drl.append("    salience 999\n");
        drl.append("    no-loop\n");
        drl.append("    when\n");
        drl.append("        not TemporalAllowed()\n");
        drl.append("    then\n");
        drl.append("        result.setDecision(\"DENY\");\n");
        drl.append("        result.setOk(false);\n");
        drl.append("        reasonCodes.add(\"TIME_WINDOW_NOT_ACTIVE\");\n");
        drl.append("        System.out.println(\"[TEMPORAL] ❌ Time window INACTIVE - DENY\");\n");
        drl.append("end\n\n");
    }

    /**
     * Extract days of week from RRULE string.
     * Example: "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR" → "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"
     */
    private String extractDaysOfWeekFromRRule(String rrule) {
        if (rrule == null || rrule.isEmpty()) {
            return "";  // No day restriction
        }

        Matcher matcher = BYDAY_PATTERN.matcher(rrule);
        if (!matcher.find()) {
            return "";  // No BYDAY clause
        }

        String byDayShort = matcher.group(1);  // e.g., "MO,TU,WE,TH,FR"

        // Convert short day names to full DayOfWeek enum names
        return java.util.Arrays.stream(byDayShort.split(","))
                .map(String::trim)
                .map(this::convertToDayOfWeek)
                .collect(Collectors.joining(","));
    }

    private String convertToDayOfWeek(String shortDay) {
        return switch (shortDay) {
            case "MO" -> "MONDAY";
            case "TU" -> "TUESDAY";
            case "WE" -> "WEDNESDAY";
            case "TH" -> "THURSDAY";
            case "FR" -> "FRIDAY";
            case "SA" -> "SATURDAY";
            case "SU" -> "SUNDAY";
            default -> throw new IllegalArgumentException("Unknown day abbreviation: " + shortDay);
        };
    }

    private boolean isSpansMidnight(String startTime, String endTime) {
        try {
            java.time.LocalTime start = java.time.LocalTime.parse(startTime);
            java.time.LocalTime end = java.time.LocalTime.parse(endTime);
            return end.isBefore(start);
        } catch (Exception e) {
            logger.warn("Failed to parse time range: start={}, end={}", startTime, endTime, e);
            return false;
        }
    }

    /**
     * Sanitize tenant ID to valid Java package name.
     * Same logic as RuleTranslationService to ensure package consistency.
     */
    private String sanitizePackageName(String tenantId) {
        // Handle reserved Java keywords and invalid package names
        if (tenantId == null || tenantId.isEmpty() || "default".equalsIgnoreCase(tenantId)) {
            return "tenant.defaulttenant";
        }

        // Convert to lowercase and replace invalid characters
        String sanitized = tenantId.toLowerCase()
                .replaceAll("[^a-z0-9_.]", "_")
                .replaceAll("^\\d", "_$0"); // Prefix with _ if starts with number

        // Ensure it doesn't start with a reserved word
        if (isReservedKeyword(sanitized)) {
            sanitized = "tenant." + sanitized;
        }

        return sanitized;
    }

    private boolean isReservedKeyword(String word) {
        return JAVA_RESERVED_KEYWORDS.contains(word);
    }

    private static final java.util.Set<String> JAVA_RESERVED_KEYWORDS = java.util.Set.of(
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const", "continue", "default",
            "do", "double", "else", "enum", "extends", "final", "finally",
            "float", "for", "goto", "if", "implements", "import", "instanceof",
            "int", "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    );
}
