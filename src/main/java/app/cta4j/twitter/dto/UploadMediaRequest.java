package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record UploadMediaRequest(
    String media,
    @JsonProperty("media_category") String mediaCategory
) {
    public UploadMediaRequest {
        Objects.requireNonNull(media);
        Objects.requireNonNull(mediaCategory);
    }
}
