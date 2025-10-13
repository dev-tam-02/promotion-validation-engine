package vn.viettel.vds.promotion.validation.engine.domain.service.compilation;

public class CompilationException extends RuntimeException {

    public CompilationException(String message) {
        super(message);
    }

    public CompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}