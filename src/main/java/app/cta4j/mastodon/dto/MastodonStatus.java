package app.cta4j.mastodon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MastodonStatus(String id, String content) {
}
