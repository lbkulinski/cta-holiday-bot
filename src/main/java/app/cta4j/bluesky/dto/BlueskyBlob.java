package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BlueskyBlob(
    @JsonProperty("$type")
    String type,

    @JsonProperty("ref")
    Reference reference,

    String mimeType,

    int size
) {
    public record Reference(
       @JsonProperty("$link")
       String link
    ) {
    }
}
