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

    // DRL template constants
    private static final String SALIENCE_1000 = "    salience 1000\n";
    private static final String WHEN = "    when\n";
    private static final String THEN = "    then\n";
    private static final String INSERT_TEMPORAL_ALLOWED = "        insert(new TemporalAllowed());\n";
    private static final String END = "end\n";
    private static final String INDENT_4_NEWLINE = "    \n";
    private static final String INDENT_8_CLOSE_BRACE = "        }\n";
    private static final String INDENT_4_CLOSE_BRACE = "    }\n";

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
            return generateEmptyTemporalDrl(tenantId);
        }

        // Extract data
        String timezone = temporalData.getTimezone() != null ? temporalData.getTimezone() : "UTC";
        String daysOfWeekCsv = extractDaysOfWeekFromRRule(temporalData.getRrule());
        List<TimeWindow> windows = temporalData.getWindows();
        String startTs = temporalData.getStartTs();
        String endTs = temporalData.getEndTs();
        String duration = temporalData.getDuration();
        String interval = temporalData.getInterval();

        logger.debug("Generating DRL: tz={}, rrule={}, daysOfWeek={}, windowsCount={}, startTs={}, endTs={}, duration={}, interval={}",
                timezone, temporalData.getRrule(), daysOfWeekCsv, windows != null ? windows.size() : 0, startTs, endTs,
                duration, interval);

        // Generate DRL content
        StringBuilder drl = new StringBuilder();
        generateHeader(drl, tenantId);
        generateCheckDateRangeFunction(drl);
        generateCheckTimeWindowFunction(drl);
        generateCheckDayOfWeekFunction(drl);
        generateCheckDurationIntervalFunction(drl);
        generateTemporalRules(drl, windows, timezone, daysOfWeekCsv, startTs, endTs,
                new Recurrence(duration, interval));

        String result = drl.toString();
        logger.info("Generated timeframe.drl for tenantId={}, assignmentId={}, size={} bytes",
                tenantId, assignmentId, result.length());
        return result;
    }

    /**
     * Generate empty temporal DRL (always allow) when no temporal constraints.
     */
    private String generateEmptyTemporalDrl(String tenantId) {
        StringBuilder drl = new StringBuilder();
        generateHeader(drl, tenantId);

        // Always allow rule (no temporal constraints)
        drl.append("rule \"temporal_always_allow\"\n");
        drl.append(SALIENCE_1000);
        drl.append(WHEN);
        drl.append("        // No temporal constraints - always allow\n");
        drl.append(THEN);
        drl.append(INSERT_TEMPORAL_ALLOWED);
        drl.append("        System.out.println(\"[TEMPORAL] ✅ No temporal constraints - ALLOWED\");\n");
        drl.append(END).append("\n");

        return drl.toString();
    }

    private void generateHeader(StringBuilder drl, String tenantId) {
        // Use tenant package name to match validation-rule.drl package
        // This ensures TemporalAllowed fact type can be shared between DRLs
        String packageName = sanitizePackageName(tenantId);

        drl.append("package ").append(packageName).append(";\n\n");

        // Imports
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;\n");
        drl.append("import vn.viettel.vds.promotion.validation.engine.domain.service.execution.FactPreparationService.ExecutionTimestamp;\n");
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

    /**
     * Generate checkDateRange function for validating campaign start/end dates.
     * Checks if the evaluation time (ExecutionTimestamp fact) is within the valid date range (startTs to endTs).
     */
    private void generateCheckDateRangeFunction(StringBuilder drl) {
        drl.append("function boolean checkDateRange(long nowMillis, String startTs, String endTs) {\n");
        drl.append("    Instant now = Instant.ofEpochMilli(nowMillis);\n");
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Check start timestamp\n");
        drl.append("    if (startTs != null && !startTs.isEmpty()) {\n");
        drl.append("        Instant start = Instant.parse(startTs);\n");
        drl.append("        if (now.isBefore(start)) {\n");
        drl.append("            System.out.println(\"[TEMPORAL] Date check: now=\" + now + \" is BEFORE startTs=\" + start);\n");
        drl.append("            return false;\n");
        drl.append(INDENT_8_CLOSE_BRACE);
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Check end timestamp\n");
        drl.append("    if (endTs != null && !endTs.isEmpty()) {\n");
        drl.append("        Instant end = Instant.parse(endTs);\n");
        drl.append("        if (now.isAfter(end)) {\n");
        drl.append("            System.out.println(\"[TEMPORAL] Date check: now=\" + now + \" is AFTER endTs=\" + end);\n");
        drl.append("            return false;\n");
        drl.append(INDENT_8_CLOSE_BRACE);
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    return true;\n");
        drl.append("}\n\n");
    }

    private void generateCheckTimeWindowFunction(StringBuilder drl) {
        drl.append("function boolean checkTimeWindow(long nowMillis, String startTime, String endTime, String timezone, boolean spansMidnight, String daysOfWeekCsv) {\n");
        drl.append("    ZonedDateTime zdt = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.of(timezone));\n");
        drl.append("    DayOfWeek currentDay = zdt.getDayOfWeek();\n");
        drl.append("    LocalTime currentTime = zdt.toLocalTime();\n");
        drl.append("    LocalTime start = LocalTime.parse(startTime);\n");
        drl.append("    LocalTime end = LocalTime.parse(endTime);\n");
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Check day of week if specified\n");
        drl.append("    if (daysOfWeekCsv != null && !daysOfWeekCsv.isEmpty()) {\n");
        drl.append("        boolean dayMatches = false;\n");
        drl.append("        String[] daysOfWeek = daysOfWeekCsv.split(\",\");\n");
        drl.append("        for (String day : daysOfWeek) {\n");
        drl.append("            if (currentDay.name().equals(day.trim())) {\n");
        drl.append("                dayMatches = true;\n");
        drl.append("                break;\n");
        drl.append("            }\n");
        drl.append(INDENT_8_CLOSE_BRACE);
        drl.append("        if (!dayMatches) return false;\n");
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Check time range\n");
        drl.append("    if (spansMidnight) {\n");
        drl.append("        return currentTime.isAfter(start) || currentTime.equals(start) || currentTime.isBefore(end) || currentTime.equals(end);\n");
        drl.append("    } else {\n");
        drl.append("        return (currentTime.isAfter(start) || currentTime.equals(start)) && (currentTime.isBefore(end) || currentTime.equals(end));\n");
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append("}\n\n");
    }

    /**
     * Generate checkDayOfWeek function for "Ngày áp dụng trong tuần" without time-of-day windows
     * (with windows, checkTimeWindow already checks the day).
     */
    private void generateCheckDayOfWeekFunction(StringBuilder drl) {
        drl.append("function boolean checkDayOfWeek(long nowMillis, String timezone, String daysOfWeekCsv) {\n");
        drl.append("    String currentDay = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.of(timezone)).getDayOfWeek().name();\n");
        drl.append("    for (String day : daysOfWeekCsv.split(\",\")) {\n");
        drl.append("        if (currentDay.equals(day.trim())) {\n");
        drl.append("            return true;\n");
        drl.append(INDENT_8_CLOSE_BRACE);
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append("    System.out.println(\"[TEMPORAL] Day check: \" + currentDay + \" is not in \" + daysOfWeekCsv);\n");
        drl.append("    return false;\n");
        drl.append("}\n\n");
    }

    /**
     * Generate checkDurationInterval function for "Lặp lại mỗi {interval} trong {duration}".
     *
     * Logic (same as Phase 2 validation-engine): cycles start at startTs and repeat every 'interval';
     * the first 'duration' of each cycle is active.
     * Example: startTs=Monday 09:00, interval=P2D, duration=PT3H
     *   - Monday 09:00-12:00 ACTIVE, Wednesday 09:00-12:00 ACTIVE, ...
     *
     * Formula:
     *   elapsed = now - startTs
     *   positionInCycle = elapsed % intervalMs
     *   isActive = positionInCycle < durationMs
     */
    private void generateCheckDurationIntervalFunction(StringBuilder drl) {
        drl.append("function boolean checkDurationInterval(long nowMillis, String startTsStr, String isoDuration, String isoInterval) {\n");
        drl.append("    long startMillis = Instant.parse(startTsStr).toEpochMilli();\n");
        drl.append("    // Parse duration (ISO 8601)\n");
        drl.append("    long durationMs;\n");
        drl.append("    if (isoDuration.contains(\"T\")) {\n");
        drl.append("        durationMs = java.time.Duration.parse(isoDuration).toMillis();\n");
        drl.append("    } else {\n");
        drl.append("        java.time.Period p = java.time.Period.parse(isoDuration);\n");
        drl.append("        durationMs = (p.getDays() * 24L * 60L * 60L * 1000L) + (p.getMonths() * 30L * 24L * 60L * 60L * 1000L) + (p.getYears() * 365L * 24L * 60L * 60L * 1000L);\n");
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Parse interval (ISO 8601)\n");
        drl.append("    long intervalMs;\n");
        drl.append("    if (isoInterval.contains(\"T\")) {\n");
        drl.append("        intervalMs = java.time.Duration.parse(isoInterval).toMillis();\n");
        drl.append("    } else {\n");
        drl.append("        java.time.Period p = java.time.Period.parse(isoInterval);\n");
        drl.append("        intervalMs = (p.getDays() * 24L * 60L * 60L * 1000L) + (p.getMonths() * 30L * 24L * 60L * 60L * 1000L) + (p.getYears() * 365L * 24L * 60L * 60L * 1000L);\n");
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Calculate position in current cycle\n");
        drl.append("    long positionInCycle = (nowMillis - startMillis) % intervalMs;\n");
        drl.append("    boolean isActive = positionInCycle < durationMs;\n");
        drl.append("    System.out.println(\"[TEMPORAL] Duration/Interval check: positionInCycle=\" + positionInCycle + \"ms, duration=\" + durationMs + \"ms, interval=\" + intervalMs + \"ms, isActive=\" + isActive);\n");
        drl.append("    return isActive;\n");
        drl.append("}\n\n");
    }

    private void generateTemporalRules(StringBuilder drl, List<TimeWindow> windows,
                                       String timezone, String daysOfWeekCsv,
                                       String startTs, String endTs, Recurrence recurrence) {

        boolean hasDateRange = (startTs != null && !startTs.isEmpty()) || (endTs != null && !endTs.isEmpty());
        boolean hasTimeWindows = windows != null && !windows.isEmpty();
        boolean hasDaysOfWeek = daysOfWeekCsv != null && !daysOfWeekCsv.isEmpty();
        // Recurrence is anchored on startTs; checkDateRange (evaluated first) already rejects times before it
        boolean hasRecurrence = recurrence.isDefined() && startTs != null && !startTs.isEmpty();

        // If no constraints at all, generate always allow rule
        if (!hasDateRange && !hasTimeWindows && !hasDaysOfWeek) {
            logger.warn("No temporal constraints defined, generating 24/7 allow rule");
            generateAlwaysAllowRule(drl);
            return;
        }

        // Escape null values for DRL string parameters
        String startTsParam = startTs != null ? startTs : "";
        String endTsParam = endTs != null ? endTs : "";

        // Create context object
        TemporalRuleContext context = new TemporalRuleContext(
                windows, timezone, daysOfWeekCsv,
                hasDateRange, hasTimeWindows,
                startTsParam, endTsParam
        );
        if (hasRecurrence) {
            context.recurrence = recurrence;
        }

        // Generate allow and deny rules
        generateAllowRule(drl, context);
        generateDenyRule(drl, hasDateRange, startTsParam, endTsParam);
    }

    private void generateAllowRule(StringBuilder drl, TemporalRuleContext context) {
        drl.append("rule \"temporal_check_allow\"\n");
        drl.append(SALIENCE_1000);
        drl.append(WHEN);

        // Build and add conditions
        java.util.List<String> conditions = buildConditions(context);

        for (String condition : conditions) {
            drl.append("        ").append(condition).append("\n");
        }

        drl.append(THEN);
        drl.append(INSERT_TEMPORAL_ALLOWED);
        drl.append(String.format("        System.out.println(\"[TEMPORAL] ✅ Temporal check PASSED - tz=%s, days=%s, startTs=%s, endTs=%s\");%n",
                context.timezone, context.daysOfWeekCsv, context.startTsParam, context.endTsParam));
        drl.append(END).append("\n");
    }

    private java.util.List<String> buildConditions(TemporalRuleContext context) {
        java.util.List<String> conditions = new java.util.ArrayList<>();

        // Evaluate against the time sent by the caller (e.g. transaction time), not the engine clock
        conditions.add("ExecutionTimestamp($now : timestamp)");

        if (context.hasDateRange) {
            conditions.add(String.format("eval(checkDateRange($now, \"%s\", \"%s\"))", context.startTsParam, context.endTsParam));
        }

        if (context.recurrence != null) {
            conditions.add(String.format("eval(checkDurationInterval($now, \"%s\", \"%s\", \"%s\"))",
                    context.startTsParam, context.recurrence.duration(), context.recurrence.interval()));
        }

        if (context.hasTimeWindows) {
            conditions.add(buildTimeWindowCondition(context.windows, context.timezone, context.daysOfWeekCsv));
        } else if (context.daysOfWeekCsv != null && !context.daysOfWeekCsv.isEmpty()) {
            conditions.add(String.format("eval(checkDayOfWeek($now, \"%s\", \"%s\"))",
                    context.timezone, context.daysOfWeekCsv));
        }

        return conditions;
    }

    private String buildTimeWindowCondition(List<TimeWindow> windows, String timezone, String daysOfWeekCsv) {
        if (windows.size() == 1) {
            return buildSingleWindowCondition(windows.get(0), timezone, daysOfWeekCsv);
        }
        return buildMultipleWindowsCondition(windows, timezone, daysOfWeekCsv);
    }

    private String buildSingleWindowCondition(TimeWindow window, String timezone, String daysOfWeekCsv) {
        boolean spansMidnight = isSpansMidnight(window.getStartTime(), window.getEndTime());
        return String.format("eval(checkTimeWindow($now, \"%s\", \"%s\", \"%s\", %s, \"%s\"))",
                window.getStartTime(), window.getEndTime(), timezone, spansMidnight, daysOfWeekCsv);
    }

    private String buildMultipleWindowsCondition(List<TimeWindow> windows, String timezone, String daysOfWeekCsv) {
        StringBuilder windowCondition = new StringBuilder("(\n");
        for (int i = 0; i < windows.size(); i++) {
            TimeWindow window = windows.get(i);
            boolean spansMidnight = isSpansMidnight(window.getStartTime(), window.getEndTime());
            if (i > 0) {
                windowCondition.append("            or\n");
            }
            windowCondition.append(String.format("            eval(checkTimeWindow($now, \"%s\", \"%s\", \"%s\", %s, \"%s\"))%n",
                    window.getStartTime(), window.getEndTime(), timezone, spansMidnight, daysOfWeekCsv));
        }
        windowCondition.append("        )");
        return windowCondition.toString();
    }

    private void generateAlwaysAllowRule(StringBuilder drl) {
        drl.append("rule \"temporal_check_allow_24_7\"\n");
        drl.append(SALIENCE_1000);
        drl.append(WHEN);
        drl.append("        // No time windows defined - allow 24/7\n");
        drl.append(THEN);
        drl.append(INSERT_TEMPORAL_ALLOWED);
        drl.append("        System.out.println(\"[TEMPORAL] ✅ 24/7 ACTIVE (no time restrictions)\");\n");
        drl.append(END).append("\n");
    }

    private void generateDenyRule(StringBuilder drl, boolean hasDateRange, String startTs, String endTs) {
        drl.append("rule \"temporal_check_deny\"\n");
        drl.append("    salience 999\n");
        drl.append("    no-loop\n");
        drl.append(WHEN);
        drl.append("        not TemporalAllowed()\n");
        drl.append(THEN);
        drl.append("        result.setDecision(\"DENY\");\n");
        drl.append("        result.setOk(false);\n");

        // Add appropriate reason code based on what constraints are defined
        if (hasDateRange) {
            drl.append("        reasonCodes.add(\"TEMPORAL_CONSTRAINT_NOT_MET\");\n");
            drl.append(String.format("        System.out.println(\"[TEMPORAL] ❌ Temporal check FAILED (startTs=%s, endTs=%s) - DENY\");%n",
                    startTs, endTs));
        } else {
            drl.append("        reasonCodes.add(\"TIME_WINDOW_NOT_ACTIVE\");\n");
            drl.append("        System.out.println(\"[TEMPORAL] ❌ Time window INACTIVE - DENY\");\n");
        }

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

    /**
     * "Lặp lại mỗi {interval} trong {duration}" settings (ISO 8601), both required to be active.
     */
    private record Recurrence(String duration, String interval) {
        boolean isDefined() {
            return duration != null && !duration.isEmpty() && interval != null && !interval.isEmpty();
        }
    }

    /**
     * Parameter object to reduce method parameter count.
     */
    private static class TemporalRuleContext {
        private final List<TimeWindow> windows;
        private final String timezone;
        private final String daysOfWeekCsv;
        private final boolean hasDateRange;
        private final boolean hasTimeWindows;
        private final String startTsParam;
        private final String endTsParam;
        // Set only when the recurrence can be anchored on startTs
        private Recurrence recurrence;

        TemporalRuleContext(List<TimeWindow> windows, String timezone, String daysOfWeekCsv,
                           boolean hasDateRange, boolean hasTimeWindows,
                           String startTsParam, String endTsParam) {
            this.windows = windows;
            this.timezone = timezone;
            this.daysOfWeekCsv = daysOfWeekCsv;
            this.hasDateRange = hasDateRange;
            this.hasTimeWindows = hasTimeWindows;
            this.startTsParam = startTsParam;
            this.endTsParam = endTsParam;
        }
    }
}
