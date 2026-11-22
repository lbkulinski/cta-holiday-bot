package com.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record Media(String id) {
    public Media {
        Objects.requireNonNull(id);
    }
}
