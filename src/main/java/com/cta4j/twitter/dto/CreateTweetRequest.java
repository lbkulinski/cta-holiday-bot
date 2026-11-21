package com.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@NullMarked
public record CreateTweetRequest(
    String text,
    @Nullable Media media
) {
    public CreateTweetRequest {
        Objects.requireNonNull(text);
    }

    public record Media(@JsonProperty("media_ids") List<String> mediaIds) {
        public Media {
            Objects.requireNonNull(mediaIds);

            mediaIds = List.copyOf(mediaIds);

            if (mediaIds.isEmpty()) {
                throw new IllegalArgumentException("mediaIds must not be empty");
            }
        }
    }
}
