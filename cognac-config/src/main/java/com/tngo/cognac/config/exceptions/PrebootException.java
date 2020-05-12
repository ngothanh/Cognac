package com.tngo.cognac.config.exceptions;

public class PrebootException extends RuntimeException {

    private static final long serialVersionUID = -9187562998289903601L;

    public PrebootException() {
        super();
    }

    public PrebootException(String message) {
        super(message);
    }

    public PrebootException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrebootException(Throwable cause) {
        super(cause);
    }
}
