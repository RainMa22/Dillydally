package me.rainma22.dillydally.sslcert.certificategetter.acme;

import me.rainma22.dillydally.exceptions.DillyDallyException;

/**
 * OrderNotReadyException
 */
public class OrderNotReadyException extends DillyDallyException{

    public OrderNotReadyException() {
    }

    public OrderNotReadyException(String message) {
        super(message);
    }

    public OrderNotReadyException(Throwable cause) {
        super(cause);
    }

    public OrderNotReadyException(String message, Throwable cause) {
        super(message, cause);
    }

}
