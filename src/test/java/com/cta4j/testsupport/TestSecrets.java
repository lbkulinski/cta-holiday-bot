package com.cta4j.testsupport;

import com.cta4j.aws.dto.Secret;

public final class TestSecrets {
    public static final String TWITTER_ACCESS_TOKEN = "tw-access";
    public static final String TWITTER_ACCESS_TOKEN_SECRET = "tw-access-secret";
    public static final String BLUESKY_IDENTIFIER = "user.bsky.social";
    public static final String BLUESKY_PASSWORD = "bsky-pw";
    public static final String MASTODON_TOKEN = "masto-token";
    public static final String MAPBOX_TOKEN = "mapbox-token";

    private TestSecrets() {
    }

    public static Secret secret() {
        return new Secret(
            new Secret.TwitterSecret("tw-key", "tw-key-secret", TWITTER_ACCESS_TOKEN, TWITTER_ACCESS_TOKEN_SECRET),
            new Secret.BlueskySecret(BLUESKY_IDENTIFIER, BLUESKY_PASSWORD),
            new Secret.MastodonSecret(MASTODON_TOKEN),
            new Secret.MapboxSecret(MAPBOX_TOKEN),
            new Secret.CtaSecret("cta-key"),
            new Secret.RollbarSecret("rollbar-token"));
    }
}
