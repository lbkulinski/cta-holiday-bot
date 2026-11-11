package app.cta4j.twitter.config;

import app.cta4j.aws.client.SecretsClient;
import app.cta4j.aws.dto.Secret;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth10aService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuth10aServiceConfig {
    @Bean
    public OAuth10aService oAuth10aService(SecretsClient secretsClient) {
        Secret.TwitterSecret secret = secretsClient.getSecret().twitter();

        return new ServiceBuilder(secret.consumerKey())
            .apiSecret(secret.consumerSecret())
            .build(TwitterApi.instance());
    }
}
