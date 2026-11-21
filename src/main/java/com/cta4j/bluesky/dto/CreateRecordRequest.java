package com.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@NullMarked
public record CreateRecordRequest(
    String repo,
    String collection,
    Record record
) {
    public CreateRecordRequest {
        Objects.requireNonNull(repo);
        Objects.requireNonNull(collection);
        Objects.requireNonNull(record);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Record(
        @JsonProperty("$type") String type,
        String text,
        Instant createdAt,
        @Nullable Embed embed
    ) {
        public Record {
            Objects.requireNonNull(type);
            Objects.requireNonNull(text);
            Objects.requireNonNull(createdAt);
        }
    }

    public record Embed(
        @JsonProperty("$type") String type,
        List<ImageBlob> images
    ) {
        public Embed {
            Objects.requireNonNull(type);
            Objects.requireNonNull(images);

            images = List.copyOf(images);

            if (images.isEmpty() || images.size() > 4) {
                throw new IllegalArgumentException("images must contain 1-4 items");
            }
        }
    }
}
