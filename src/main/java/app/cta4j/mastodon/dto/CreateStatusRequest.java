package app.cta4j.mastodon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CreateStatusRequest(
    String status,

    @JsonProperty("media_ids")
    List<String> mediaIds
) {
}
