package com.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record Record(
    String cid,
    String uri
) {
    public Record {
        Objects.requireNonNull(cid);
        Objects.requireNonNull(uri);
    }
}
