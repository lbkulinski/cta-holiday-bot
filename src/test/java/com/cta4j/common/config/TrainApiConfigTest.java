package com.cta4j.common.config;

import com.cta4j.aws.client.SecretsClient;
import com.cta4j.testsupport.TestSecrets;
import com.cta4j.train.TrainApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainApiConfigTest {
    @Mock
    private SecretsClient secretsClient;

    @Test
    void buildsTrainApiUsingKeyFromSecrets() {
        when(this.secretsClient.getSecret()).thenReturn(TestSecrets.secret());

        TrainApi api = new TrainApiConfig().trainApi(this.secretsClient);

        assertThat(api).isNotNull();
        assertThat(api.follow()).isNotNull();
        verify(this.secretsClient).getSecret();
    }
}
