package vn.viettel.vds.promotion.rule.engine.domain.exception;

public class SessionCreationException extends RuntimeException {
    private final String bundleHash;

    public SessionCreationException(String message, String bundleHash) {
        super(message);
        this.bundleHash = bundleHash;
    }

    public SessionCreationException(String message, String bundleHash, Throwable cause) {
        super(message, cause);
        this.bundleHash = bundleHash;
    }

    public String getBundleHash() {
        return bundleHash;
    }
}
