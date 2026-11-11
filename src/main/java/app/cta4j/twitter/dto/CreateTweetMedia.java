package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;

@NullMarked
public record CreateTweetMedia(@JsonProperty("media_ids") List<String> mediaIds) {
    public CreateTweetMedia {
        Objects.requireNonNull(mediaIds);

        mediaIds = List.copyOf(mediaIds);

        if (mediaIds.isEmpty()) {
            throw new IllegalArgumentException("mediaIds must not be empty");
        }
    }
}
