package vn.viettel.vds.promotion.rule.engine.application.usecase;

public class RuleExecutionException extends RuntimeException {

    public RuleExecutionException(String message) {
        super(message);
    }

    public RuleExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}