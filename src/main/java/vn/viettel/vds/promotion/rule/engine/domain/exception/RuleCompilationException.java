package vn.viettel.vds.promotion.rule.engine.domain.exception;

public class RuleCompilationException extends RuntimeException {
    private final String tenantId;
    private final String ruleId;
    private final String version;

    public RuleCompilationException(String message, String tenantId, String ruleId, String version) {
        super(message);
        this.tenantId = tenantId;
        this.ruleId = ruleId;
        this.version = version;
    }

    public RuleCompilationException(String message, String tenantId, String ruleId, String version, Throwable cause) {
        super(message, cause);
        this.tenantId = tenantId;
        this.ruleId = ruleId;
        this.version = version;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getVersion() {
        return version;
    }
}
