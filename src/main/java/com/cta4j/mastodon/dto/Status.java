package com.cta4j.mastodon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record Status(String id) {
    public Status {
        Objects.requireNonNull(id);
    }
}
