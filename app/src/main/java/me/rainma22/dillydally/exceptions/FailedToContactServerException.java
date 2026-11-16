package me.rainma22.dillydally.exceptions;

public class FailedToContactServerException extends DillyDallyException {

    public FailedToContactServerException() {
    }

    public FailedToContactServerException(String message) {
        super(message);
    }

    public FailedToContactServerException(Throwable cause) {
        super(cause);
    }

    public FailedToContactServerException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
