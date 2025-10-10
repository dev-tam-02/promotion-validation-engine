package vn.viettel.vds.promotion.validation.engine.domain.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Service for validating time windows
 */
@Service
public class TimeWindowService {

    /**
     * Check if current time is within an active time window
     *
     * @param policyId The time policy identifier
     * @param timezone The timezone to use for time checks
     * @return true if current time is within the active window
     */
    public boolean isActiveNow(String policyId, String timezone) {
        // TODO: Implement actual time window logic
        // For now, return true to allow rules to pass
        // In production, this should:
        // 1. Fetch the time policy by policyId
        // 2. Check if current time (in given timezone) falls within policy's active windows
        // 3. Consider day of week, time ranges, etc.

        return true;
    }

    /**
     * Check if a specific instant is within an active time window
     *
     * @param policyId The time policy identifier
     * @param instant The instant to check
     * @param timezone The timezone to use
     * @return true if the instant is within the active window
     */
    public boolean isActive(String policyId, Instant instant, String timezone) {
        ZonedDateTime zdt = instant.atZone(ZoneId.of(timezone));

        // TODO: Implement actual time window validation
        // This is a placeholder implementation
        return true;
    }
}
