package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record UploadMediaResponse(TwitterMedia data) {
    public UploadMediaResponse {
        Objects.requireNonNull(data);
    }
}
