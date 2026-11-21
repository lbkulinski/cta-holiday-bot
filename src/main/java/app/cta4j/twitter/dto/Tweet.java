package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record Tweet(
    String id,
    String text
) {
    public Tweet {
        Objects.requireNonNull(id);
        Objects.requireNonNull(text);
    }
}
