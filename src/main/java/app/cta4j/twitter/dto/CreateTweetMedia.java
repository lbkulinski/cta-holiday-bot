package app.cta4j.twitter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CreateTweetMedia(@JsonProperty("media_ids")List<String> mediaIds) {
}
