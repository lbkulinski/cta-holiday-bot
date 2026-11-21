package com.cta4j.bluesky.dto;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record CreateSessionRequest(
    String identifier,
    String password
) {
    public CreateSessionRequest {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(password);
    }
}
