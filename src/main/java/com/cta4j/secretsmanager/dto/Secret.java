package com.cta4j.secretsmanager.dto;

import lombok.Data;

public record Secret(TwitterSecret twitter, BlueskySecret bluesky, MapboxSecret mapbox, CtaSecret cta) {
    @Data
    public static final class TwitterSecret {
        private String clientId;
        private String clientSecret;
        private String accessToken;
        private String refreshToken;
    }

    public record BlueskySecret(String appPassword) {}

    public record MapboxSecret(String accessToken) {}

    public record CtaSecret(String trainApiKey) {}
}
