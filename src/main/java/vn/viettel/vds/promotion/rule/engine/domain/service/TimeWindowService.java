package vn.viettel.vds.promotion.rule.engine.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.application.port.out.TimePolicyRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.domain.model.TimePolicy;

import java.time.*;

/**
 * Service for validating time windows against time policies
 */
@Service
@RequiredArgsConstructor
public class TimeWindowService {

    private final TimePolicyRepositoryPort timePolicyRepository;

    /**
     * Check if current time is within an active time window
     *
     * @param policyId The time policy identifier
     * @param timezone The timezone to use for time checks (e.g., "Asia/Ho_Chi_Minh")
     * @return true if current time is within the active window
     */
    public boolean isActiveNow(String policyId, String timezone) {
        return isActive(policyId, Instant.now(), timezone);
    }

    /**
     * Check if a specific instant is within an active time window
     *
     * @param policyId The time policy identifier
     * @param instant  The instant to check
     * @param timezone The timezone to use
     * @return true if the instant is within the active window
     */
    public boolean isActive(String policyId, Instant instant, String timezone) {
        // Check if policy is active
        if (!timePolicyRepository.isActive(policyId)) {
            return false;
        }

        // Get the time policy
        TimePolicy policy = timePolicyRepository.findById(policyId)
                .orElse(null);

        if (policy == null || !policy.isActive() || policy.getTimeWindows().isEmpty()) {
            return false;
        }

        // Convert instant to zoned date time
        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime zdt = instant.atZone(zoneId);
        DayOfWeek currentDay = zdt.getDayOfWeek();
        LocalTime currentTime = zdt.toLocalTime();

        // Check if current time falls within any time window
        for (TimePolicy.TimeWindow window : policy.getTimeWindows()) {
            if (isTimeInWindow(currentDay, currentTime, window)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a specific day and time falls within a time window
     */
    private boolean isTimeInWindow(DayOfWeek day, LocalTime time, TimePolicy.TimeWindow window) {
        // Check day of week constraint
        if (!window.getDaysOfWeek().isEmpty() && !window.getDaysOfWeek().contains(day)) {
            return false;
        }

        // Check time range
        LocalTime startTime = window.getStartTime();
        LocalTime endTime = window.getEndTime();

        if (window.isSpansMidnight()) {
            // Time window spans midnight (e.g., 23:00 to 02:00)
            return time.isAfter(startTime) || time.equals(startTime) ||
                    time.isBefore(endTime) || time.equals(endTime);
        } else {
            // Normal time window (e.g., 09:00 to 17:00)
            return (time.isAfter(startTime) || time.equals(startTime)) &&
                    (time.isBefore(endTime) || time.equals(endTime));
        }
    }
}
