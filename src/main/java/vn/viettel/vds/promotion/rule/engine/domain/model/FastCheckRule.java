package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Domain models for fast check rule components.
 * These are simple data structures that can be quickly evaluated.
 */
public class FastCheckRule {

    private FastCheckRule() {
        // Private constructor to prevent instantiation
    }

    /**
     * Blackout period when promotions are not allowed
     */
    public static class BlackoutPeriod {
        private String name;
        private Instant start;
        private Instant end;
        private String reason;

        public BlackoutPeriod() {
        }

        public BlackoutPeriod(String name, Instant start, Instant end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Instant getStart() {
            return start;
        }

        public void setStart(Instant start) {
            this.start = start;
        }

        public Instant getEnd() {
            return end;
        }

        public void setEnd(Instant end) {
            this.end = end;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /**
     * Business hours when promotions are allowed
     */
    public static class BusinessHours {
        private LocalTime start;
        private LocalTime end;
        private String timezone;

        public BusinessHours() {
        }

        public BusinessHours(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
            this.timezone = "Asia/Ho_Chi_Minh";
        }

        // Getters and setters
        public LocalTime getStart() {
            return start;
        }

        public void setStart(LocalTime start) {
            this.start = start;
        }

        public LocalTime getEnd() {
            return end;
        }

        public void setEnd(LocalTime end) {
            this.end = end;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }
    }
}