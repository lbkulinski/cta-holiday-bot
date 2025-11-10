package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateTweetRequest(
    String text,

    CreateTweetMedia media
) {
}
