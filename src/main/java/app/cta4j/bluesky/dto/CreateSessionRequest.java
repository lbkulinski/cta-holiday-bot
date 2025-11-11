package app.cta4j.bluesky.dto;

public record CreateSessionRequest(
    String identifier,
    String password
) {
}
