package vn.viettel.vds.promotion.validation.engine.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;
import vn.viettel.vds.promotion.validation.engine.domain.service.execution.FactPreparationService.ExecutionTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Compiles and runs the generated timeframe.drl against an explicit evaluation time (ExecutionTimestamp fact),
 * so results do not depend on the machine clock (PROM-366).
 * 2026-09-14 is a Monday; times are Asia/Ho_Chi_Minh (+07:00).
 */
class TemporalDrlEvaluationTimeTest {

    private static final String TENANT = "tenant1";
    private static final String TIMEZONE = "Asia/Ho_Chi_Minh";
    private static final String ALLOW = "ALLOW";
    private static final String DENY = "DENY";

    private static final String BUSINESS_RULE = """
            package tenant1;

            import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;

            global ValidationResult result;
            global java.util.List reasonCodes;

            rule "business_allow_when_temporal_allowed"
                salience 0
            when
                TemporalAllowed()
            then
                result.setDecision("ALLOW");
                result.setOk(true);
            end
            """;

    private final TemporalDrlGenerator generator = new TemporalDrlGenerator();
    private final DroolsCompilationService compilationService = new DroolsCompilationService();

    @Test
    @DisplayName("Time window is judged at the transaction time, not at the processing time")
    void timeWindowUsesTransactionTime() {
        // "Giờ áp dụng trong ngày" 09:00-17:00 on Monday
        CompileRequest.TemporalPolicyData data = temporalData("FREQ=WEEKLY;BYDAY=MO", "09:00", "17:00");
        KieContainer container = compile(data);

        // Transaction Monday 08:59 (processed 09:01) -> reject; transaction Monday 16:59 (processed 17:01) -> pass
        assertEquals(DENY, decide(container, "2026-09-14T08:59:00+07:00"));
        assertEquals(ALLOW, decide(container, "2026-09-14T16:59:00+07:00"));
        assertEquals(DENY, decide(container, "2026-09-14T20:00:00+07:00"));
        // Not a selected day
        assertEquals(DENY, decide(container, "2026-09-15T10:00:00+07:00"));
    }

    @Test
    @DisplayName("Several time windows (one per selected day) are OR-ed and judged at the transaction time")
    void multipleTimeWindowsUseTransactionTime() {
        CompileRequest.TemporalPolicyData data = temporalData("FREQ=WEEKLY;BYDAY=MO", "09:00", "12:00");
        data.setWindows(List.of(
                new CompileRequest.TimeWindow("09:00", "12:00"),
                new CompileRequest.TimeWindow("13:00", "17:00")));
        KieContainer container = compile(data);

        assertEquals(ALLOW, decide(container, "2026-09-14T10:00:00+07:00"));
        assertEquals(DENY, decide(container, "2026-09-14T12:30:00+07:00"));
        assertEquals(ALLOW, decide(container, "2026-09-14T16:59:00+07:00"));
        assertEquals(DENY, decide(container, "2026-09-14T17:01:00+07:00"));
    }

    @Test
    @DisplayName("Repeat every 2 days for 3 hours from Monday 09:00")
    void recurringWindowFollowsReviewerExample() {
        CompileRequest.TemporalPolicyData data = temporalData(null, "00:00", "23:59");
        data.setStartTs("2026-09-14T02:00:00Z");
        data.setInterval("P2D");
        data.setDuration("PT3H");
        KieContainer container = compile(data);

        assertEquals(ALLOW, decide(container, "2026-09-14T10:00:00+07:00"));
        assertEquals(DENY, decide(container, "2026-09-14T14:00:00+07:00"));
        assertEquals(DENY, decide(container, "2026-09-15T10:00:00+07:00"));
        assertEquals(ALLOW, decide(container, "2026-09-16T10:00:00+07:00"));
        // Before the campaign start
        assertEquals(DENY, decide(container, "2026-09-14T08:30:00+07:00"));
    }

    @Test
    @DisplayName("Repeat window combined with days of week must satisfy both")
    void recurringWindowAlsoRespectsDaysOfWeek() {
        CompileRequest.TemporalPolicyData data = temporalData("FREQ=WEEKLY;BYDAY=MO", "00:00", "23:59");
        data.setStartTs("2026-09-14T02:00:00Z");
        data.setInterval("P2D");
        data.setDuration("PT3H");
        KieContainer container = compile(data);

        assertEquals(ALLOW, decide(container, "2026-09-14T10:00:00+07:00"));
        // Active repeat window but Wednesday is not a selected day
        assertEquals(DENY, decide(container, "2026-09-16T10:00:00+07:00"));
    }

    @Test
    @DisplayName("Days of week without time windows are judged at the transaction time (PROM-1585)")
    void daysOfWeekWithoutTimeWindows() {
        // "Ngày áp dụng trong tuần": Monday and Wednesday, whole day, within the campaign dates
        CompileRequest.TemporalPolicyData data = new CompileRequest.TemporalPolicyData();
        data.setTimezone(TIMEZONE);
        data.setRrule("FREQ=WEEKLY;BYDAY=MO,WE");
        data.setStartTs("2026-09-01T00:00:00Z");
        data.setEndTs("2026-12-31T00:00:00Z");
        KieContainer container = compile(data);

        assertEquals(ALLOW, decide(container, "2026-09-14T00:00:00+07:00"));
        assertEquals(ALLOW, decide(container, "2026-09-16T23:59:00+07:00"));
        assertEquals(DENY, decide(container, "2026-09-15T10:00:00+07:00"));
        // Monday 23:30 in UTC is already Tuesday in the campaign timezone
        assertEquals(DENY, decide(container, "2026-09-14T17:30:00Z"));
        // Selected day but outside the campaign dates
        assertEquals(DENY, decide(container, "2026-08-31T10:00:00+07:00"));
    }

    @Test
    @DisplayName("Days of week alone (no dates, no time windows) still restrict the day (PROM-1585)")
    void daysOfWeekOnly() {
        CompileRequest.TemporalPolicyData data = new CompileRequest.TemporalPolicyData();
        data.setTimezone(TIMEZONE);
        data.setRrule("FREQ=WEEKLY;BYDAY=MO");
        KieContainer container = compile(data);

        assertEquals(ALLOW, decide(container, "2026-09-14T10:00:00+07:00"));
        assertEquals(DENY, decide(container, "2026-09-15T10:00:00+07:00"));
    }

    @Test
    @DisplayName("Repeat window without time windows also respects days of week (PROM-1585)")
    void recurringWindowWithoutTimeWindowsRespectsDaysOfWeek() {
        CompileRequest.TemporalPolicyData data = new CompileRequest.TemporalPolicyData();
        data.setTimezone(TIMEZONE);
        data.setRrule("FREQ=WEEKLY;BYDAY=MO");
        data.setStartTs("2026-09-14T02:00:00Z");
        data.setInterval("P2D");
        data.setDuration("PT3H");
        KieContainer container = compile(data);

        assertEquals(ALLOW, decide(container, "2026-09-14T10:00:00+07:00"));
        // Active repeat window but Wednesday is not a selected day
        assertEquals(DENY, decide(container, "2026-09-16T10:00:00+07:00"));
    }

    @Test
    @DisplayName("Repeat window without time windows or days of week (PROM-1585)")
    void recurringWindowWithoutTimeWindows() {
        CompileRequest.TemporalPolicyData data = new CompileRequest.TemporalPolicyData();
        data.setTimezone(TIMEZONE);
        data.setStartTs("2026-09-14T02:00:00Z");
        data.setInterval("P2D");
        data.setDuration("PT3H");
        KieContainer container = compile(data);

        assertEquals(ALLOW, decide(container, "2026-09-14T10:00:00+07:00"));
        assertEquals(DENY, decide(container, "2026-09-14T14:00:00+07:00"));
        assertEquals(DENY, decide(container, "2026-09-15T10:00:00+07:00"));
        assertEquals(ALLOW, decide(container, "2026-09-16T10:00:00+07:00"));
    }

    private CompileRequest.TemporalPolicyData temporalData(String rrule, String startTime, String endTime) {
        CompileRequest.TemporalPolicyData data = new CompileRequest.TemporalPolicyData();
        data.setTimezone(TIMEZONE);
        data.setRrule(rrule);
        data.setWindows(List.of(new CompileRequest.TimeWindow(startTime, endTime)));
        return data;
    }

    private KieContainer compile(CompileRequest.TemporalPolicyData data) {
        Map<String, String> drlFiles = new LinkedHashMap<>();
        drlFiles.put("timeframe.drl", generator.generateTimeframeDrl(TENANT, "assignment-366", data));
        drlFiles.put("business-rule.drl", BUSINESS_RULE);
        DroolsCompilationService.CompilationResult compiled =
                compilationService.compileMultipleDrls(TENANT, "bundle-366", 1, drlFiles);
        return compilationService.createKieContainer(compiled.getArtifactBytes());
    }

    private String decide(KieContainer container, String evaluationTime) {
        KieSession session = container.newKieSession();
        try {
            ValidationResult result = new ValidationResult();
            List<String> reasonCodes = new ArrayList<>();
            session.setGlobal("result", result);
            session.setGlobal("reasonCodes", reasonCodes);
            session.insert(new ExecutionTimestamp(OffsetDateTime.parse(evaluationTime).toInstant().toEpochMilli()));
            session.fireAllRules();
            return result.getDecision();
        } finally {
            session.dispose();
        }
    }
}
