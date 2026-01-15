package vn.viettel.vds.promotion.rule.engine.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for TemporalDrlGenerator
 */
class TemporalDrlGeneratorTest {

    private TemporalDrlGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TemporalDrlGenerator();
    }

    @Test
    @DisplayName("Should generate DRL with single time window")
    void testGenerateDrlWithSingleTimeWindow() {
        // Given
        String assignmentId = "test-assignment-001";
        CompileRequest.TemporalPolicyData temporalData = createTemporalData(
                "Asia/Bangkok",
                "FREQ=DAILY",
                List.of(new CompileRequest.TimeWindow("09:00", "17:00"))
        );

        // When
        String drl = generator.generateTimeframeDrl(assignmentId, temporalData);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("package rules"));
        assertTrue(drl.contains("function boolean checkTimeWindow"));
        assertTrue(drl.contains("rule \"temporal_check_allow\""));
        assertTrue(drl.contains("salience 1000"));
        assertTrue(drl.contains("eval(checkTimeWindow(\"09:00\", \"17:00\", \"Asia/Bangkok\", false, \"\"))"));
        assertTrue(drl.contains("insert(new TemporalAllowed())"));
        assertTrue(drl.contains("rule \"temporal_check_deny\""));
        assertTrue(drl.contains("salience 999"));
        assertTrue(drl.contains("not TemporalAllowed()"));
        assertTrue(drl.contains("reasonCodes.add(\"TIME_WINDOW_NOT_ACTIVE\")"));
    }

    @Test
    @DisplayName("Should generate DRL with multiple time windows (OR logic)")
    void testGenerateDrlWithMultipleTimeWindows() {
        // Given
        String assignmentId = "test-assignment-002";
        List<CompileRequest.TimeWindow> windows = List.of(
                new CompileRequest.TimeWindow("09:00", "12:00"),
                new CompileRequest.TimeWindow("13:00", "17:00"),
                new CompileRequest.TimeWindow("18:00", "21:00")
        );
        CompileRequest.TemporalPolicyData temporalData = createTemporalData(
                "Asia/Ho_Chi_Minh",
                "FREQ=DAILY",
                windows
        );

        // When
        String drl = generator.generateTimeframeDrl(assignmentId, temporalData);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("09:00"));
        assertTrue(drl.contains("12:00"));
        assertTrue(drl.contains("13:00"));
        assertTrue(drl.contains("17:00"));
        assertTrue(drl.contains("18:00"));
        assertTrue(drl.contains("21:00"));
        assertTrue(drl.contains("or")); // OR logic between windows
        assertTrue(drl.contains("Asia/Ho_Chi_Minh"));
    }

    @Test
    @DisplayName("Should generate DRL with day-of-week filtering (BYDAY)")
    void testGenerateDrlWithDayOfWeekFiltering() {
        // Given
        String assignmentId = "test-assignment-003";
        CompileRequest.TemporalPolicyData temporalData = createTemporalData(
                "UTC",
                "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", // Weekdays only
                List.of(new CompileRequest.TimeWindow("08:00", "18:00"))
        );

        // When
        String drl = generator.generateTimeframeDrl(assignmentId, temporalData);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"));
        assertTrue(drl.contains("08:00"));
        assertTrue(drl.contains("18:00"));
    }

    @Test
    @DisplayName("Should generate DRL with midnight-spanning time range")
    void testGenerateDrlWithMidnightSpanningRange() {
        // Given
        String assignmentId = "test-assignment-004";
        CompileRequest.TemporalPolicyData temporalData = createTemporalData(
                "Asia/Bangkok",
                "FREQ=DAILY",
                List.of(new CompileRequest.TimeWindow("22:00", "06:00")) // Spans midnight
        );

        // When
        String drl = generator.generateTimeframeDrl(assignmentId, temporalData);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("22:00"));
        assertTrue(drl.contains("06:00"));
        assertTrue(drl.contains("true")); // spansMidnight = true
    }

    @Test
    @DisplayName("Should generate empty temporal DRL when no temporal data provided")
    void testGenerateEmptyDrlWhenNoTemporalData() {
        // Given
        String assignmentId = "test-assignment-005";

        // When
        String drl = generator.generateTimeframeDrl(assignmentId, null);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("package rules"));
        assertTrue(drl.contains("rule \"temporal_always_allow\""));
        assertTrue(drl.contains("salience 1000"));
        assertTrue(drl.contains("No temporal constraints - always allow"));
        assertTrue(drl.contains("insert(new TemporalAllowed())"));
    }

    @Test
    @DisplayName("Should generate 24/7 allow rule when no time windows defined")
    void testGenerate24x7AllowRuleWhenNoTimeWindows() {
        // Given
        String assignmentId = "test-assignment-006";
        CompileRequest.TemporalPolicyData temporalData = createTemporalData(
                "Asia/Bangkok",
                "FREQ=DAILY",
                new ArrayList<>() // Empty windows list
        );

        // When
        String drl = generator.generateTimeframeDrl(assignmentId, temporalData);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("rule \"temporal_check_allow_24_7\""));
        assertTrue(drl.contains("No time windows defined - allow 24/7"));
        assertTrue(drl.contains("insert(new TemporalAllowed())"));
    }

    @Test
    @DisplayName("Should handle weekend-only schedule (Saturday and Sunday)")
    void testGenerateDrlWithWeekendSchedule() {
        // Given
        String assignmentId = "test-assignment-007";
        CompileRequest.TemporalPolicyData temporalData = createTemporalData(
                "Asia/Bangkok",
                "FREQ=WEEKLY;BYDAY=SA,SU", // Weekend only
                List.of(new CompileRequest.TimeWindow("00:00", "23:59"))
        );

        // When
        String drl = generator.generateTimeframeDrl(assignmentId, temporalData);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("SATURDAY,SUNDAY"));
        assertTrue(drl.contains("00:00"));
        assertTrue(drl.contains("23:59"));
    }

    @Test
    @DisplayName("Should use default package name")
    void testDefaultPackageName() {
        // Given
        String assignmentId = "test-assignment-001";
        CompileRequest.TemporalPolicyData temporalData = createTemporalData(
                "UTC",
                "FREQ=DAILY",
                List.of(new CompileRequest.TimeWindow("09:00", "17:00"))
        );

        // When
        String drl = generator.generateTimeframeDrl(assignmentId, temporalData);

        // Then
        assertNotNull(drl);
        // Package should be the default "rules"
        assertTrue(drl.contains("package rules"));
    }

    @Test
    @DisplayName("Should include timezone in DRL for time validation")
    void testIncludeTimezoneInDrl() {
        // Given
        String assignmentId = "test-assignment-008";
        CompileRequest.TemporalPolicyData temporalData = createTemporalData(
                "America/New_York",
                "FREQ=DAILY",
                List.of(new CompileRequest.TimeWindow("09:00", "17:00"))
        );

        // When
        String drl = generator.generateTimeframeDrl(assignmentId, temporalData);

        // Then
        assertNotNull(drl);
        assertTrue(drl.contains("America/New_York"));
        assertTrue(drl.contains("ZoneId.of(timezone)"));
    }

    // Helper methods
    private CompileRequest.TemporalPolicyData createTemporalData(String timezone, String rrule, List<CompileRequest.TimeWindow> windows) {
        CompileRequest.TemporalPolicyData data = new CompileRequest.TemporalPolicyData();
        data.setTimezone(timezone);
        data.setRrule(rrule);
        data.setWindows(windows);
        return data;
    }
}
