package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@NullMarked
public record CreateTweetRequest(
    String text,
    @Nullable CreateTweetMedia media
) {
    public CreateTweetRequest {
        Objects.requireNonNull(text);
    }
}
