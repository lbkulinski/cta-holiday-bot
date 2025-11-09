package app.cta4j.common.dto;

import java.time.Instant;

public record Secret(
    TwitterSecret twitter,
    BlueskySecret bluesky,
    MapboxSecret mapbox,
    CtaSecret cta,
    RollbarSecret rollbar
) {
    public record TwitterSecret(
        String clientId,
        String clientSecret,
        String accessToken,
        String refreshToken,
        Instant expirationTime
    ) {}

    public record BlueskySecret(String identifier, String appPassword) {
    }

    public record MapboxSecret(String accessToken) {
    }

    public record CtaSecret(String trainApiKey) {
    }

    public record RollbarSecret(String accessToken) {
    }

    public Secret withTwitterTokens(String accessToken, String refreshToken, Instant expirationTime) {
        TwitterSecret twitterSecret = new TwitterSecret(
            this.twitter.clientId,
            this.twitter.clientSecret,
            accessToken,
            refreshToken,
            expirationTime
        );

        return new Secret(twitterSecret, this.bluesky, this.mapbox, this.cta, this.rollbar);
    }
}
