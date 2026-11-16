package app.cta4j.post.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Objects;

@NullMarked
public record Post(String text, @Nullable Image image) {
    public Post {
        Objects.requireNonNull(text);
    }

    public record Image(File file, String altText) {
        public Image {
            Objects.requireNonNull(file);
            Objects.requireNonNull(altText);
        }
    }
}
