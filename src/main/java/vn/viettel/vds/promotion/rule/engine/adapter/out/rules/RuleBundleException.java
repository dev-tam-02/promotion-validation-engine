package vn.viettel.vds.promotion.rule.engine.adapter.out.rules;

/**
 * Exception thrown when there are errors loading or building rule bundles.
 */
public class RuleBundleException extends RuntimeException {

    public RuleBundleException(String message) {
        super(message);
    }

    public RuleBundleException(String message, Throwable cause) {
        super(message, cause);
    }
}
