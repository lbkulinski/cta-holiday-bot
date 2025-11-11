package app.cta4j.mastodon.exception;

public class MastodonException extends RuntimeException {
    public MastodonException(String message) {
        super(message);
    }

    public MastodonException(String message, Throwable cause) {
        super(message, cause);
    }
}
