package vn.viettel.vds.promotion.validation.engine.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Domain model representing a time-based policy for promotions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimePolicy {

    private String id;
    private String name;
    private String description;

    // Time windows when this policy is active
    private List<TimeWindow> timeWindows;

    // Status
    private boolean active;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeWindow {
        // Days of week when this window is active (null or empty means all days)
        private Set<DayOfWeek> daysOfWeek;

        // Start time (inclusive)
        private LocalTime startTime;

        // End time (exclusive)
        private LocalTime endTime;

        // If true, the window spans midnight (e.g., 22:00 to 02:00)
        private boolean spansMidnight;
    }
}
