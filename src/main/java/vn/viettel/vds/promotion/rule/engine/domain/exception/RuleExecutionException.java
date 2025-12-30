package vn.viettel.vds.promotion.rule.engine.domain.exception;

public class RuleExecutionException extends RuntimeException {
    private final String bundleHash;
    private final String executionId;

    public RuleExecutionException(String message, String bundleHash, String executionId) {
        super(message);
        this.bundleHash = bundleHash;
        this.executionId = executionId;
    }

    public RuleExecutionException(String message, String bundleHash, String executionId, Throwable cause) {
        super(message, cause);
        this.bundleHash = bundleHash;
        this.executionId = executionId;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public String getExecutionId() {
        return executionId;
    }
}
