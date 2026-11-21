package com.cta4j.twitter.dto;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record MediaMetadata(String id) {
    public MediaMetadata {
        Objects.requireNonNull(id);
    }
}
