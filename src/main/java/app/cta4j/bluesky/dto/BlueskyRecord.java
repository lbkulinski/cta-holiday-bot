package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BlueskyRecord(
    String uri,
    String cid
) {
}
