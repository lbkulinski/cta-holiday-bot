package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Blob(
    @JsonAlias("$type")
    String type,

    @JsonAlias("ref")
    Reference reference,

    String mimeType,

    int size
) {
    public record Reference(
       @JsonAlias("$link")
       String link
    ) {}
}
