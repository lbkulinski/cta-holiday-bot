package app.cta4j.bluesky.dto;

import java.time.Instant;

public record CreateRecordData(
    String text,
    Instant createdAt,
    Embed embed
) {
}
