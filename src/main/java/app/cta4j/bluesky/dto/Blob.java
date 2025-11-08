package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    ) {
    }
}
