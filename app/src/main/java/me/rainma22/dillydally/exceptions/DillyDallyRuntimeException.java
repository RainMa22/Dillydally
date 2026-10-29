package me.rainma22.dillydally.exceptions;

/**
 * DillyDallyRuntimeException
 */
public class DillyDallyRuntimeException extends RuntimeException {

    public DillyDallyRuntimeException() {
    }

    public DillyDallyRuntimeException(String message) {
        super(message);
    }

    public DillyDallyRuntimeException(Throwable cause) {
        super(cause);
    }

    public DillyDallyRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}
