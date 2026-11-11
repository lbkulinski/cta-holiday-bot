package app.cta4j.common.dto;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Objects;

@NullMarked
public record Post(String text, @Nullable File media) {
    public Post {
        Objects.requireNonNull(text);
    }
}
