package com.cta4j.mastodon.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@NullMarked
public record CreateStatusRequest(
    String status,

    @JsonProperty("media_ids")
    @Nullable
    List<String> mediaIds
) {
    public CreateStatusRequest {
        Objects.requireNonNull(status);

        if (mediaIds != null) {
            mediaIds = List.copyOf(mediaIds);
        }
    }
}
