package app.cta4j.twitter.exception;

public final class TwitterException extends RuntimeException {
    public TwitterException(String message, Throwable cause) {
        super(message, cause);
    }

    public TwitterException(String message) {
        super(message);
    }
}
