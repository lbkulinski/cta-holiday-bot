package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record CreateMediaMetadataResponse(MediaMetadata data) {
    public CreateMediaMetadataResponse {
        Objects.requireNonNull(data);
    }
}
