package app.cta4j.bluesky.dto;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record CreateBlueskySessionRequest(
    String identifier,
    String password
) {
    public CreateBlueskySessionRequest {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(password);
    }
}
