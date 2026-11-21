package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record UploadBlobResponse(Blob blob) {
    public UploadBlobResponse {
        Objects.requireNonNull(blob);
    }
}
