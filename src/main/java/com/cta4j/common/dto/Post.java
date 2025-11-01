package com.cta4j.common.dto;

import java.io.File;
import java.util.Objects;

public record Post(String text, File media) {
    public Post {
        Objects.requireNonNull(text);
    }
}
