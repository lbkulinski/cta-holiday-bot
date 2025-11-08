package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Embed(
    @JsonProperty("$type")
    String type,

    List<Image> images
) {
}
