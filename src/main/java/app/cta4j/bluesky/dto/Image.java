package app.cta4j.bluesky.dto;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record Image(
    Blob image,
    String alt
) {
    public Image {
        Objects.requireNonNull(image);
        Objects.requireNonNull(alt);
    }
}
