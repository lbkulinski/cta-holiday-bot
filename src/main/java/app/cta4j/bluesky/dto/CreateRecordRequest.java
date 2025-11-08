package app.cta4j.bluesky.dto;

public record CreateRecordRequest(
    String repo,
    String collection,
    CreateRecordData record
) {
}
