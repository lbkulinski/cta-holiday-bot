package com.cta4j.common.dto;

import java.io.File;
import java.util.Objects;

public record PostPayload(String text, File media) {
    public PostPayload {
        Objects.requireNonNull(text);
    }
}
