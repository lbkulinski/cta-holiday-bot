package com.cta4j.common.dto;

public record Secret(TwitterSecret twitter, BlueskySecret bluesky, MapboxSecret mapbox, CtaSecret cta) {
    public record TwitterSecret(
        String clientId,
        String clientSecret,
        String accessToken,
        String refreshToken
    ) {}

    public record BlueskySecret(String appPassword) {}

    public record MapboxSecret(String accessToken) {}

    public record CtaSecret(String trainApiKey) {}

    public Secret withTwitterTokens(String accessToken, String refreshToken) {
        TwitterSecret twitterSecret = new TwitterSecret(
            this.twitter.clientId,
            this.twitter.clientSecret,
            accessToken,
            refreshToken
        );

        return new Secret(twitterSecret, this.bluesky, this.mapbox, this.cta);
    }
}
