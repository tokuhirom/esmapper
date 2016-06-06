package me.geso.esmapper.exception;

public class EsmapperRuntimeException extends RuntimeException {
    public EsmapperRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public EsmapperRuntimeException(Throwable cause) {
        super(cause);
    }

    public EsmapperRuntimeException(String message) {
        super(message);
    }
}
