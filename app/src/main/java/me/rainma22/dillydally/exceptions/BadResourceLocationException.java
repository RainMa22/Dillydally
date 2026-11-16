package me.rainma22.dillydally.exceptions;

/**
 * BadResourceLocationException
 */
public class BadResourceLocationException extends InvalidJSONResponseException{

    public BadResourceLocationException() {
    }

    public BadResourceLocationException(String message) {
        super(message);
    }

    public BadResourceLocationException(Throwable cause) {
        super(cause);
    }

    public BadResourceLocationException(String message, Throwable cause) {
        super(message, cause);
    }

}
