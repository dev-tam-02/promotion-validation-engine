package vn.viettel.vds.promotion.rule.engine.domain.exception;

public class RuleCompilationException extends RuntimeException {
    private final String ruleId;
    private final String version;

    public RuleCompilationException(String message, String ruleId, String version) {
        super(message);
        this.ruleId = ruleId;
        this.version = version;
    }

    public RuleCompilationException(String message, String ruleId, String version, Throwable cause) {
        super(message, cause);
        this.ruleId = ruleId;
        this.version = version;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getVersion() {
        return version;
    }
}
