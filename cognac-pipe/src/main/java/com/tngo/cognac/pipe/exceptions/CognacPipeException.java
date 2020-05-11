package com.tngo.cognac.pipe.exceptions;

public class CognacPipeException extends RuntimeException {

    private static final long serialVersionUID = -8657575463126197521L;

    public CognacPipeException(String message, Throwable cause) {
        super(message, cause);
    }

    public CognacPipeException(String message) {
        super(message);
    }
}
