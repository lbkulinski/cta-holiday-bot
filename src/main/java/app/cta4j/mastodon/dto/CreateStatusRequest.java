package app.cta4j.mastodon.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateStatusRequest(
    String status,

    @JsonProperty("media_ids")
    List<String> mediaIds
) {
}
