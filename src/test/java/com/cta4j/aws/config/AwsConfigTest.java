package com.cta4j.aws.config;

import com.amazonaws.secretsmanager.caching.SecretCache;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AwsConfigTest {
    @Test
    void regionConfigParsesRegionName() {
        Region region = new RegionConfig().region("us-east-2");

        assertThat(region).isEqualTo(Region.US_EAST_2);
    }

    @Test
    void regionConfigRejectsBlankRegion() {
        assertThatThrownBy(() -> new RegionConfig().region(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void secretsManagerClientUsesConfiguredRegion() {
        try (SecretsManagerClient client = new SecretsManagerClientConfig().secretsManagerClient(Region.US_EAST_2)) {
            assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.US_EAST_2);
        }
    }

    @Test
    void secretCacheIsCreated() {
        var client = mock(SecretsManagerClient.class);

        try (SecretCache cache = new SecretCacheConfig().secretCache(client)) {
            assertThat(cache).isNotNull();
        }
    }
}
