package me.rainma22.dillydally.exceptions;

public class InvalidJSONResponseException extends DillyDallyException {

    public InvalidJSONResponseException() {
    }

    public InvalidJSONResponseException(String message) {
        super(message);
    }

    public InvalidJSONResponseException(Throwable cause) {
        super(cause);
    }

    public InvalidJSONResponseException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
