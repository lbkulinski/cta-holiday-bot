package com.cta4j.dto;

import lombok.Data;

public record Secret(TwitterSecret twitter, BlueskySecret bluesky) {
    @Data
    public static final class TwitterSecret {
        private String clientId;
        private String clientSecret;
        private String accessToken;
        private String refreshToken;
    }

    @Data
    public static final class BlueskySecret {
        private String appPassword;
    }
}
