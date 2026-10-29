package me.rainma22.dillydally.exceptions;

public class InvalidResponseException extends DillyDallyException {

    public InvalidResponseException() {
    }

    public InvalidResponseException(String message) {
        super(message);
    }

    public InvalidResponseException(Throwable cause) {
        super(cause);
    }

    public InvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
