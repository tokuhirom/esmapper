package me.geso.esmapper.exception;

public class EsmapperException extends Exception {
    public EsmapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public EsmapperException(Throwable cause) {
        super(cause);
    }
}
