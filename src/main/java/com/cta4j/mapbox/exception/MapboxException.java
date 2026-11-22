package com.cta4j.mapbox.exception;

public final class MapboxException extends RuntimeException {
    public MapboxException(String message, Throwable cause) {
        super(message, cause);
    }

    public MapboxException(String message) {
        super(message);
    }
}
