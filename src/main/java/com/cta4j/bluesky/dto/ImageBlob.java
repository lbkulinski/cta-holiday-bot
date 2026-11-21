package com.cta4j.bluesky.dto;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record ImageBlob(
    Blob image,
    String alt
) {
    public ImageBlob {
        Objects.requireNonNull(image);
        Objects.requireNonNull(alt);
    }
}
