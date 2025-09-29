package vn.viettel.vds.promotion.validation.engine.application.dto;

import java.time.Instant;
import java.util.List;

public class CompileJobResponse {

    private String id;
    private String tenantId;
    private String ruleId;
    private Integer targetVersion;
    private String status;
    private String requestedBy;
    private Instant requestedAt;
    private Instant completedAt;
    private String bundleHash;
    private List<LogEntry> logs;
    private List<String> errors;

    public CompileJobResponse() {}

    public CompileJobResponse(String id, String tenantId, String ruleId, Integer targetVersion,
                             String status, String requestedBy, Instant requestedAt,
                             Instant completedAt, String bundleHash, List<LogEntry> logs,
                             List<String> errors) {
        this.id = id;
        this.tenantId = tenantId;
        this.ruleId = ruleId;
        this.targetVersion = targetVersion;
        this.status = status;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.completedAt = completedAt;
        this.bundleHash = bundleHash;
        this.logs = logs;
        this.errors = errors;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Integer getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(Integer targetVersion) {
        this.targetVersion = targetVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public void setLogs(List<LogEntry> logs) {
        this.logs = logs;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    // Nested class
    public static class LogEntry {
        private String level;
        private String msg;
        private Instant timestamp;

        public LogEntry() {}

        public LogEntry(String level, String msg, Instant timestamp) {
            this.level = level;
            this.msg = msg;
            this.timestamp = timestamp;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }
}