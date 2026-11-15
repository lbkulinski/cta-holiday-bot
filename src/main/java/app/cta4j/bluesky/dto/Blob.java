package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record Blob(
    Ref ref,
    String mimeType,
    long size
) {
    public Blob {
        Objects.requireNonNull(ref);
        Objects.requireNonNull(mimeType);

        if (size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
    }

    public record Ref(@JsonProperty("$link") String link) {
        public Ref {
            Objects.requireNonNull(link);
        }
    }
}
