package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TwitterMedia(
    String id,

    @JsonAlias("media_key")
    String mediaKey
) {
}
