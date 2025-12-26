package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

public class KieSessionCreationException extends RuntimeException {

    public KieSessionCreationException(String message) {
        super(message);
    }

    public KieSessionCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}