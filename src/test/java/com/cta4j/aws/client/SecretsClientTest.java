package com.cta4j.aws.client;

import com.amazonaws.secretsmanager.caching.SecretCache;
import com.cta4j.aws.dto.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecretsClientTest {
    private static final String SECRET_ID = "my-secret-id";

    private static final String VALID_JSON = """
        {
          "twitter": {"consumerKey": "a", "consumerKeySecret": "b", "accessToken": "c", "accessTokenSecret": "d"},
          "bluesky": {"identifier": "user.bsky.social", "appPassword": "pw"},
          "mastodon": {"accessToken": "m"},
          "mapbox": {"accessToken": "mb"},
          "cta": {"trainApiKey": "k"},
          "rollbar": {"accessToken": "r"},
          "unknown": "ignored"
        }
        """;

    @Mock
    private SecretCache secretCache;

    private SecretsClient client;

    @BeforeEach
    void setUp() {
        this.client = new SecretsClient(this.secretCache, JsonMapper.builder().build(), SECRET_ID);
    }

    @Test
    void deserializesSecretAndIgnoresUnknownProperties() {
        when(this.secretCache.getSecretString(SECRET_ID)).thenReturn(VALID_JSON);

        Secret secret = this.client.getSecret();

        assertThat(secret.twitter().consumerKey()).isEqualTo("a");
        assertThat(secret.bluesky().identifier()).isEqualTo("user.bsky.social");
        assertThat(secret.bluesky().appPassword()).isEqualTo("pw");
        assertThat(secret.mastodon().accessToken()).isEqualTo("m");
        assertThat(secret.mapbox().accessToken()).isEqualTo("mb");
        assertThat(secret.cta().trainApiKey()).isEqualTo("k");
        assertThat(secret.rollbar().accessToken()).isEqualTo("r");
        verify(this.secretCache).getSecretString(SECRET_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void rejectsNullOrBlankSecretString(String secretString) {
        when(this.secretCache.getSecretString(SECRET_ID)).thenReturn(secretString);

        assertThatThrownBy(this.client::getSecret)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Secret string is null or blank");
    }

    @Test
    void wrapsMalformedJson() {
        when(this.secretCache.getSecretString(SECRET_ID)).thenReturn("{not json");

        assertThatThrownBy(this.client::getSecret)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Failed to deserialize secret JSON string")
            .hasCauseInstanceOf(tools.jackson.core.JacksonException.class);
    }

    @Test
    void wrapsJsonMissingRequiredSections() {
        when(this.secretCache.getSecretString(SECRET_ID)).thenReturn("{}");

        assertThatThrownBy(this.client::getSecret).isInstanceOf(IllegalStateException.class);
    }
}
