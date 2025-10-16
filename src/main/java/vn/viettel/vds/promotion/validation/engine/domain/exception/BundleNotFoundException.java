package vn.viettel.vds.promotion.validation.engine.domain.exception;

public class BundleNotFoundException extends RuntimeException {

    public BundleNotFoundException(String bundleHash) {
        super("Bundle not found: " + bundleHash);
    }

    public BundleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
