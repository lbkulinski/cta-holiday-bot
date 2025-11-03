package app.cta4j.bluesky.exception;

public class BlueskyException extends RuntimeException {
    public BlueskyException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlueskyException(String message) {
        super(message);
    }
}
