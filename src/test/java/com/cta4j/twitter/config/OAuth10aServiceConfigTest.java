package com.cta4j.twitter.config;

import com.cta4j.aws.client.SecretsClient;
import com.cta4j.testsupport.TestSecrets;
import com.github.scribejava.core.oauth.OAuth10aService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth10aServiceConfigTest {
    @Mock
    private SecretsClient secretsClient;

    @Test
    void buildsServiceWithConsumerCredentialsFromSecrets() throws Exception {
        when(this.secretsClient.getSecret()).thenReturn(TestSecrets.secret());

        try (OAuth10aService service = new OAuth10aServiceConfig().oAuth10aService(this.secretsClient)) {
            assertThat(service.getApiKey()).isEqualTo("tw-key");
            assertThat(service.getApiSecret()).isEqualTo("tw-key-secret");
        }
    }
}
