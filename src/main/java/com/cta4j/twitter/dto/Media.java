package com.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Media(
    String id,

    @JsonAlias("media_key")
    String mediaKey
) {
}
