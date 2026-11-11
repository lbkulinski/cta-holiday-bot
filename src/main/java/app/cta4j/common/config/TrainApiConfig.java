package app.cta4j.common.config;

import app.cta4j.aws.client.SecretsClient;
import com.cta4j.train.TrainApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrainApiConfig {
    @Bean
    public TrainApi trainApi(SecretsClient secretsClient) {
        String trainApiKey = secretsClient.getSecret()
                                          .cta()
                                          .trainApiKey();

        return TrainApi.builder(trainApiKey)
                       .build();
    }
}
