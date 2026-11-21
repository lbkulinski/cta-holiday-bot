package com.cta4j.mastodon.exception;

public final class MastodonException extends RuntimeException {
    public MastodonException(String message, Throwable cause) {
        super(message, cause);
    }
}
