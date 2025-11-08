package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateRecordData(
    String text,
    Instant createdAt,
    Embed embed
) {
}
