package com.tngo.cognac.config.exceptions;

public class CognacConfigException extends RuntimeException {

    private static final long serialVersionUID = -9187562998289903601L;

    public CognacConfigException() {
        super();
    }

    public CognacConfigException(String message) {
        super(message);
    }

    public CognacConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public CognacConfigException(Throwable cause) {
        super(cause);
    }
}
