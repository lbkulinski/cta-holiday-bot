package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record CreateMediaMetadataRequest(
    String id,
    Metadata metadata
) {
    public CreateMediaMetadataRequest {
        Objects.requireNonNull(id);
        Objects.requireNonNull(metadata);
    }

    public record Metadata(@JsonProperty("alt_text") AltText altText) {
        public Metadata {
            Objects.requireNonNull(altText);
        }
    }

    public record AltText(String text) {
        public AltText {
            Objects.requireNonNull(text);
        }
    }
}
