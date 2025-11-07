package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Session(
    String accessJwt,
    String refreshJwt,
    String handle,
    String did
) {}
