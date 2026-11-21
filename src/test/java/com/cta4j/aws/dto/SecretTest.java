package com.cta4j.aws.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SecretTest {
    private static Secret.TwitterSecret twitter() {
        return new Secret.TwitterSecret("ck-value", "cks-value", "at-value", "ats-value");
    }

    private static Secret.BlueskySecret bluesky() {
        return new Secret.BlueskySecret("user.bsky.social", "bsky-password");
    }

    private static Secret.MastodonSecret mastodon() {
        return new Secret.MastodonSecret("mastodon-token");
    }

    private static Secret.MapboxSecret mapbox() {
        return new Secret.MapboxSecret("mapbox-token");
    }

    private static Secret.CtaSecret cta() {
        return new Secret.CtaSecret("cta-key");
    }

    private static Secret.RollbarSecret rollbar() {
        return new Secret.RollbarSecret("rollbar-token");
    }

    @Test
    void twitterToStringMasksAllValues() {
        assertThat(twitter().toString())
            .doesNotContain("ck-value", "cks-value", "at-value", "ats-value")
            .contains("***");
    }

    @Test
    void blueskyToStringShowsIdentifierButMasksPassword() {
        assertThat(bluesky().toString())
            .contains("user.bsky.social")
            .doesNotContain("bsky-password");
    }

    @Test
    void singleTokenSecretsMaskTheirToken() {
        assertThat(mastodon().toString()).doesNotContain("mastodon-token");
        assertThat(mapbox().toString()).doesNotContain("mapbox-token");
        assertThat(cta().toString()).doesNotContain("cta-key");
        assertThat(rollbar().toString()).doesNotContain("rollbar-token");
    }

    @Test
    void secretToStringDoesNotLeakNestedValues() {
        var secret = new Secret(twitter(), bluesky(), mastodon(), mapbox(), cta(), rollbar());

        assertThat(secret.toString()).doesNotContain(
            "ck-value", "cks-value", "at-value", "ats-value", "bsky-password",
            "mastodon-token", "mapbox-token", "cta-key", "rollbar-token");
    }

    @Test
    void secretRejectsNullComponents() {
        assertThatNullPointerException()
            .isThrownBy(() -> new Secret(null, bluesky(), mastodon(), mapbox(), cta(), rollbar()));
        assertThatNullPointerException()
            .isThrownBy(() -> new Secret(twitter(), null, mastodon(), mapbox(), cta(), rollbar()));
        assertThatNullPointerException()
            .isThrownBy(() -> new Secret(twitter(), bluesky(), null, mapbox(), cta(), rollbar()));
        assertThatNullPointerException()
            .isThrownBy(() -> new Secret(twitter(), bluesky(), mastodon(), null, cta(), rollbar()));
        assertThatNullPointerException()
            .isThrownBy(() -> new Secret(twitter(), bluesky(), mastodon(), mapbox(), null, rollbar()));
        assertThatNullPointerException()
            .isThrownBy(() -> new Secret(twitter(), bluesky(), mastodon(), mapbox(), cta(), null));
    }

    @Test
    void nestedSecretsRejectNulls() {
        assertThatNullPointerException().isThrownBy(() -> new Secret.TwitterSecret(null, "b", "c", "d"));
        assertThatNullPointerException().isThrownBy(() -> new Secret.TwitterSecret("a", null, "c", "d"));
        assertThatNullPointerException().isThrownBy(() -> new Secret.TwitterSecret("a", "b", null, "d"));
        assertThatNullPointerException().isThrownBy(() -> new Secret.TwitterSecret("a", "b", "c", null));
        assertThatNullPointerException().isThrownBy(() -> new Secret.BlueskySecret(null, "p"));
        assertThatNullPointerException().isThrownBy(() -> new Secret.BlueskySecret("i", null));
        assertThatNullPointerException().isThrownBy(() -> new Secret.MastodonSecret(null));
        assertThatNullPointerException().isThrownBy(() -> new Secret.MapboxSecret(null));
        assertThatNullPointerException().isThrownBy(() -> new Secret.CtaSecret(null));
        assertThatNullPointerException().isThrownBy(() -> new Secret.RollbarSecret(null));
    }
}
