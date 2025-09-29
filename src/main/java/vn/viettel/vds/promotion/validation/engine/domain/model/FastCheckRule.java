package vn.viettel.vds.promotion.validation.engine.domain.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Domain models for fast check rule components.
 * These are simple data structures that can be quickly evaluated.
 */
public class FastCheckRule {

    /**
     * Blackout period when promotions are not allowed
     */
    public static class BlackoutPeriod {
        private String name;
        private LocalDateTime start;
        private LocalDateTime end;
        private String reason;

        public BlackoutPeriod() {}

        public BlackoutPeriod(String name, LocalDateTime start, LocalDateTime end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public LocalDateTime getStart() { return start; }
        public void setStart(LocalDateTime start) { this.start = start; }

        public LocalDateTime getEnd() { return end; }
        public void setEnd(LocalDateTime end) { this.end = end; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * Business hours when promotions are allowed
     */
    public static class BusinessHours {
        private LocalTime start;
        private LocalTime end;
        private String timezone;

        public BusinessHours() {}

        public BusinessHours(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
            this.timezone = "Asia/Ho_Chi_Minh";
        }

        // Getters and setters
        public LocalTime getStart() { return start; }
        public void setStart(LocalTime start) { this.start = start; }

        public LocalTime getEnd() { return end; }
        public void setEnd(LocalTime end) { this.end = end; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }
}