package vn.viettel.vds.promotion.rule.engine.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileRequest.TemporalPolicyData;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileRequest.TimeWindow;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service to generate temporal DRL (timeframe.drl) from temporal policy data.
 * <p>
 * Responsible for:
 * - Generating Drools DRL syntax for temporal validation
 * - Converting RFC 5545 RRULE to Drools conditions
 * - Creating time window checking logic
 * <p>
 * Generated DRL structure:
 * 1. checkTimeWindow() function for time validation
 * 2. temporal_check_allow rule (salience 1000) - checks if current time is within allowed windows
 * 3. temporal_check_deny rule (salience 999) - denies if temporal check failed
 */
@Service
public class TemporalDrlGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TemporalDrlGenerator.class);
    private static final Pattern BYDAY_PATTERN = Pattern.compile("BYDAY=([A-Z,]+)");
    private static final String DEFAULT_PACKAGE = "rules";

    // DRL template constants
    private static final String SALIENCE_1000 = "    salience 1000\n";
    private static final String WHEN = "    when\n";
    private static final String THEN = "    then\n";
    private static final String INSERT_TEMPORAL_ALLOWED = "        insert(new TemporalAllowed());\n";
    private static final String END = "end\n";
    private static final String INDENT_4_NEWLINE = "    \n";
    private static final String INDENT_8_CLOSE_BRACE = "        }\n";
    private static final String INDENT_4_CLOSE_BRACE = "    }\n";
    private static final String END_DOUBLE_NEWLINE = "end\n\n";
    private static final String RETURN_FALSE_INDENT_12 = "            return false;\n";
    private static final String CLOSE_BRACE_DOUBLE_NEWLINE = "}\n\n";
    private static final String ELSE_BLOCK = "    } else {\n";
    /**
     * Generate timeframe.drl from temporal policy data.
     *
     * @param assignmentId assignment identifier (for logging)
     * @param temporalData temporal policy data
     * @return DRL string content
     */
    public String generateTimeframeDrl(String assignmentId, TemporalPolicyData temporalData) {
        logger.info("Generating timeframe.drl for assignmentId={}", assignmentId);

        if (temporalData == null) {
            logger.warn("No temporal data provided for assignmentId={}, generating empty temporal DRL", assignmentId);
            return generateEmptyTemporalDrl();
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
                timezone, temporalData.getRrule(), daysOfWeekCsv, windows != null ? windows.size() : 0, startTs, endTs, duration, interval);

        // Generate DRL content
        StringBuilder drl = new StringBuilder();
        generateHeader(drl);

        // Check if duration/interval mode is enabled
        boolean hasDurationInterval = duration != null && !duration.isEmpty()
                && interval != null && !interval.isEmpty()
                && startTs != null && !startTs.isEmpty();

        if (hasDurationInterval) {
            // Duration/Interval mode: recurring time windows based on startTs
            logger.info("Using Duration/Interval mode: duration={}, interval={}, startTs={}", duration, interval, startTs);
            generateDurationIntervalCheckFunction(drl);
            generateDurationIntervalRules(drl, startTs, endTs, duration, interval);
        } else {
            // Legacy mode: fixed time windows
            generateCheckDateRangeFunction(drl);
            generateCheckTimeWindowFunction(drl);
            generateTemporalRules(drl, windows, timezone, daysOfWeekCsv, startTs, endTs);
        }

        String result = drl.toString();
        logger.info("Generated timeframe.drl for assignmentId={}, size={} bytes",
                assignmentId, result.length());
        return result;
    }

    /**
     * Generate empty temporal DRL (always allow) when no temporal constraints.
     */
    private String generateEmptyTemporalDrl() {
        StringBuilder drl = new StringBuilder();
        generateHeader(drl);

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

    private void generateHeader(StringBuilder drl) {
        drl.append("package ").append(DEFAULT_PACKAGE).append(";\n\n");

        // Imports
        drl.append("import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;\n");
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
        drl.append(END_DOUBLE_NEWLINE);
    }

    /**
     * Generate checkDateRange function for validating campaign start/end dates.
     * Checks if current timestamp is within the valid date range (startTs to endTs).
     */
    private void generateCheckDateRangeFunction(StringBuilder drl) {
        drl.append("function boolean checkDateRange(String startTs, String endTs) {\n");
        drl.append("    Instant now = Instant.now();\n");
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Check start timestamp\n");
        drl.append("    if (startTs != null && !startTs.isEmpty()) {\n");
        drl.append("        Instant start = Instant.parse(startTs);\n");
        drl.append("        if (now.isBefore(start)) {\n");
        drl.append("            System.out.println(\"[TEMPORAL] Date check: now=\" + now + \" is BEFORE startTs=\" + start);\n");
        drl.append(RETURN_FALSE_INDENT_12);
        drl.append(INDENT_8_CLOSE_BRACE);
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Check end timestamp\n");
        drl.append("    if (endTs != null && !endTs.isEmpty()) {\n");
        drl.append("        Instant end = Instant.parse(endTs);\n");
        drl.append("        if (now.isAfter(end)) {\n");
        drl.append("            System.out.println(\"[TEMPORAL] Date check: now=\" + now + \" is AFTER endTs=\" + end);\n");
        drl.append(RETURN_FALSE_INDENT_12);
        drl.append(INDENT_8_CLOSE_BRACE);
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    return true;\n");
        drl.append(CLOSE_BRACE_DOUBLE_NEWLINE);
    }

    private void generateCheckTimeWindowFunction(StringBuilder drl) {
        drl.append("function boolean checkTimeWindow(String startTime, String endTime, String timezone, boolean spansMidnight, String daysOfWeekCsv) {\n");
        drl.append("    ZonedDateTime zdt = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.of(timezone));\n");
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
        drl.append(ELSE_BLOCK);
        drl.append("        return (currentTime.isAfter(start) || currentTime.equals(start)) && (currentTime.isBefore(end) || currentTime.equals(end));\n");
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(CLOSE_BRACE_DOUBLE_NEWLINE);
    }

    /**
     * Generate checkDurationInterval function for recurring time windows.
     * <p>
     * Logic: Campaign starts at startTs, active for 'duration' every 'interval'.
     * Example: duration=PT1H, interval=P1D, startTs=2025-12-08T09:00:00Z
     * - Day 1: 09:00-10:00 ACTIVE
     * - Day 2: 09:00-10:00 ACTIVE
     * - etc.
     * <p>
     * Formula:
     * elapsed = now - startTs
     * positionInCycle = elapsed % intervalMs
     * isActive = positionInCycle < durationMs
     */
    private void generateDurationIntervalCheckFunction(StringBuilder drl) {
        drl.append("function boolean checkDurationInterval(String startTsStr, String endTsStr, String isoDuration, String isoInterval) {\n");
        drl.append("    long now = System.currentTimeMillis();\n");
        drl.append("    long startTs = java.time.Instant.parse(startTsStr).toEpochMilli();\n");
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Check if campaign has started\n");
        drl.append("    if (now < startTs) {\n");
        drl.append("        System.out.println(\"[TEMPORAL] Campaign not started yet: startTs=\" + startTsStr + \", now=\" + now);\n");
        drl.append("        return false;\n");
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Check if campaign has ended (if endTs is specified)\n");
        drl.append("    if (endTsStr != null && !endTsStr.isEmpty()) {\n");
        drl.append("        long endTs = java.time.Instant.parse(endTsStr).toEpochMilli();\n");
        drl.append("        if (now > endTs) {\n");
        drl.append("            System.out.println(\"[TEMPORAL] Campaign ended: endTs=\" + endTsStr + \", now=\" + now);\n");
        drl.append(RETURN_FALSE_INDENT_12);
        drl.append(INDENT_8_CLOSE_BRACE);
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Parse duration (ISO 8601)\n");
        drl.append("    long durationMs;\n");
        drl.append("    if (isoDuration.contains(\"T\")) {\n");
        drl.append("        durationMs = java.time.Duration.parse(isoDuration).toMillis();\n");
        drl.append(ELSE_BLOCK);
        drl.append("        java.time.Period p = java.time.Period.parse(isoDuration);\n");
        drl.append("        durationMs = (p.getDays() * 24L * 60L * 60L * 1000L) + (p.getMonths() * 30L * 24L * 60L * 60L * 1000L) + (p.getYears() * 365L * 24L * 60L * 60L * 1000L);\n");
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Parse interval (ISO 8601)\n");
        drl.append("    long intervalMs;\n");
        drl.append("    if (isoInterval.contains(\"T\")) {\n");
        drl.append("        intervalMs = java.time.Duration.parse(isoInterval).toMillis();\n");
        drl.append(ELSE_BLOCK);
        drl.append("        java.time.Period p = java.time.Period.parse(isoInterval);\n");
        drl.append("        intervalMs = (p.getDays() * 24L * 60L * 60L * 1000L) + (p.getMonths() * 30L * 24L * 60L * 60L * 1000L) + (p.getYears() * 365L * 24L * 60L * 60L * 1000L);\n");
        drl.append(INDENT_4_CLOSE_BRACE);
        drl.append(INDENT_4_NEWLINE);
        drl.append("    // Calculate position in current cycle\n");
        drl.append("    long elapsed = now - startTs;\n");
        drl.append("    long positionInCycle = elapsed % intervalMs;\n");
        drl.append("    boolean isActive = positionInCycle < durationMs;\n");
        drl.append(INDENT_4_NEWLINE);
        drl.append("    System.out.println(\"[TEMPORAL] Duration/Interval check: elapsed=\" + elapsed + \"ms, cycle=\" + (elapsed / intervalMs) + \", positionInCycle=\" + positionInCycle + \"ms, duration=\" + durationMs + \"ms, interval=\" + intervalMs + \"ms, isActive=\" + isActive);\n");
        drl.append(INDENT_4_NEWLINE);
        drl.append("    return isActive;\n");
        drl.append(CLOSE_BRACE_DOUBLE_NEWLINE);
    }

    /**
     * Generate rules for Duration/Interval mode.
     */
    private void generateDurationIntervalRules(StringBuilder drl, String startTs, String endTs,
                                               String duration, String interval) {
        String endTsParam = endTs != null ? endTs : "";

        // Allow rule
        drl.append("rule \"temporal_duration_interval_allow\"\n");
        drl.append(SALIENCE_1000);
        drl.append(WHEN);
        drl.append(String.format("        eval(checkDurationInterval(\"%s\", \"%s\", \"%s\", \"%s\"))%n",
                startTs, endTsParam, duration, interval));
        drl.append(THEN);
        drl.append(INSERT_TEMPORAL_ALLOWED);
        drl.append(String.format("        System.out.println(\"[TEMPORAL] ✅ Duration/Interval check PASSED - startTs=%s, duration=%s, interval=%s\");%n",
                startTs, duration, interval));
        drl.append(END).append("\n");

        // Deny rule
        drl.append("rule \"temporal_duration_interval_deny\"\n");
        drl.append("    salience 999\n");
        drl.append("    no-loop\n");
        drl.append(WHEN);
        drl.append("        not TemporalAllowed()\n");
        drl.append(THEN);
        drl.append("        result.setDecision(\"DENY\");\n");
        drl.append("        result.setOk(false);\n");
        drl.append("        reasonCodes.add(\"OUTSIDE_ACTIVE_WINDOW\");\n");
        drl.append(String.format("        System.out.println(\"[TEMPORAL] ❌ Outside active window (duration=%s, interval=%s) - DENY\");%n",
                duration, interval));
        drl.append(END_DOUBLE_NEWLINE);
    }

    private void generateTemporalRules(StringBuilder drl, List<TimeWindow> windows,
                                       String timezone, String daysOfWeekCsv,
                                       String startTs, String endTs) {

        boolean hasDateRange = (startTs != null && !startTs.isEmpty()) || (endTs != null && !endTs.isEmpty());
        boolean hasTimeWindows = windows != null && !windows.isEmpty();

        // If no constraints at all, generate always allow rule
        if (!hasDateRange && !hasTimeWindows) {
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

        if (context.hasDateRange) {
            conditions.add(String.format("eval(checkDateRange(\"%s\", \"%s\"))", context.startTsParam, context.endTsParam));
        }

        if (context.hasTimeWindows) {
            conditions.add(buildTimeWindowCondition(context.windows, context.timezone, context.daysOfWeekCsv));
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
        return String.format("eval(checkTimeWindow(\"%s\", \"%s\", \"%s\", %s, \"%s\"))",
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
            windowCondition.append(String.format("            eval(checkTimeWindow(\"%s\", \"%s\", \"%s\", %s, \"%s\"))%n",
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

        drl.append(END_DOUBLE_NEWLINE);
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
