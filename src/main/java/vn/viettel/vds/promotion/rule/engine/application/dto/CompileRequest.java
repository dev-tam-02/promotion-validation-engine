package vn.viettel.vds.promotion.rule.engine.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

public class CompileRequest {

    @NotBlank
    private String tenantId;

    @NotBlank
    private String ruleId;

    @NotNull
    @Positive
    private Integer version;

    @NotBlank
    private String logic;

    @NotNull
    private List<Map<String, Object>> nodes;

    private String operatorsFingerprint;

    @Valid
    private Limits limits;

    private List<TimeLink> timeLinks;

    public CompileRequest() {
        // Used by frameworks for deserialization.
    }


    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

    public List<Map<String, Object>> getNodes() {
        return nodes;
    }

    public void setNodes(List<Map<String, Object>> nodes) {
        this.nodes = nodes;
    }

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits;
    }

    public List<TimeLink> getTimeLinks() {
        return timeLinks;
    }

    public void setTimeLinks(List<TimeLink> timeLinks) {
        this.timeLinks = timeLinks;
    }

    public String getOperatorsFingerprint() {
        return operatorsFingerprint;
    }

    public void setOperatorsFingerprint(String operatorsFingerprint) {
        this.operatorsFingerprint = operatorsFingerprint;
    }

    // Nested classes
    public static class Limits {
        private Integer perCustomer;
        private Integer perDay;

        public Limits() {
        }

        public Limits(Integer perCustomer, Integer perDay) {
            this.perCustomer = perCustomer;
            this.perDay = perDay;
        }

        public Integer getPerCustomer() {
            return perCustomer;
        }

        public void setPerCustomer(Integer perCustomer) {
            this.perCustomer = perCustomer;
        }

        public Integer getPerDay() {
            return perDay;
        }

        public void setPerDay(Integer perDay) {
            this.perDay = perDay;
        }
    }

    public static class TimeLink {
        @NotBlank
        private String policyId;

        @NotBlank
        private String mode;

        @Valid
        private TemporalPolicyData data;  // Actual temporal policy data for DRL generation

        public TimeLink() {
        }

        public TimeLink(String policyId, String mode, TemporalPolicyData data) {
            this.policyId = policyId;
            this.mode = mode;
            this.data = data;
        }

        public String getPolicyId() {
            return policyId;
        }

        public void setPolicyId(String policyId) {
            this.policyId = policyId;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public TemporalPolicyData getData() {
            return data;
        }

        public void setData(TemporalPolicyData data) {
            this.data = data;
        }
    }

    /**
     * Temporal policy data for generating timeframe.drl
     */
    public static class TemporalPolicyData {
        private String timezone;        // e.g., "Asia/Bangkok"
        private String rrule;           // RFC 5545 RRULE, e.g., "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
        private String startTs;         // ISO 8601, e.g., "2024-01-01T00:00:00Z"
        private String endTs;           // ISO 8601, e.g., "2024-12-31T23:59:59Z"
        private List<TimeWindow> windows;  // Time-of-day windows
        private String duration;        // ISO 8601 Duration, e.g., "PT1H" (1 hour active window)
        private String interval;        // ISO 8601 Duration, e.g., "P1D" (repeat every 1 day)

        public TemporalPolicyData() {
            // Empty constructor for JSON deserialization
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public String getRrule() {
            return rrule;
        }

        public void setRrule(String rrule) {
            this.rrule = rrule;
        }

        public String getStartTs() {
            return startTs;
        }

        public void setStartTs(String startTs) {
            this.startTs = startTs;
        }

        public String getEndTs() {
            return endTs;
        }

        public void setEndTs(String endTs) {
            this.endTs = endTs;
        }

        public List<TimeWindow> getWindows() {
            return windows;
        }

        public void setWindows(List<TimeWindow> windows) {
            this.windows = windows;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public String getInterval() {
            return interval;
        }

        public void setInterval(String interval) {
            this.interval = interval;
        }
    }

    /**
     * Time window within a day (e.g., 9AM-5PM)
     */
    public static class TimeWindow {
        @NotBlank
        private String startTime;  // "HH:mm" format, e.g., "09:00"

        @NotBlank
        private String endTime;    // "HH:mm" format, e.g., "17:00"

        public TimeWindow() {
        }

        public TimeWindow(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
    }
}