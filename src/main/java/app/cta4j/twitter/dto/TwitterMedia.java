package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record TwitterMedia(
    String id,
    @JsonAlias("media_key") String mediaKey
) {
    public TwitterMedia {
        Objects.requireNonNull(id);
        Objects.requireNonNull(mediaKey);
    }
}
