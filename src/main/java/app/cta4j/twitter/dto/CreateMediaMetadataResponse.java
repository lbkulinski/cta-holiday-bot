package app.cta4j.twitter.dto;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public record CreateMediaMetadataResponse(@Nullable MediaMetadata data) {
    public CreateMediaMetadataResponse {
        Objects.requireNonNull(data);
    }
}
