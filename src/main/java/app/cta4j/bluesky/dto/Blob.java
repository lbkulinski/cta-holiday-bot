package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record Blob(Ref ref) {
    public Blob {
        Objects.requireNonNull(ref);
    }

    public record Ref(@JsonProperty("$link") String link) {
        public Ref {
            Objects.requireNonNull(link);
        }
    }
}
