package com.cta4j.exception;

public class TwitterServiceException extends RuntimeException {
    public TwitterServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public TwitterServiceException(String message) {
        super(message);
    }
}
