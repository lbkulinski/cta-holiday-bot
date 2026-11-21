package com.cta4j.twitter.exception;

public final class TwitterException extends RuntimeException {
    public TwitterException(String message, Throwable cause) {
        super(message, cause);
    }
}
