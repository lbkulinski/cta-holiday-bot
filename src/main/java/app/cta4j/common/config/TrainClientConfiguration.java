package app.cta4j.common.config;

import app.cta4j.common.dto.Secret;
import app.cta4j.common.service.SecretService;
import com.cta4j.train.client.TrainClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrainClientConfiguration {
    private final Secret secret;

    @Autowired
    public TrainClientConfiguration(SecretService secretService) {
        this.secret = secretService.getSecret();
    }

    @Bean
    public TrainClient trainClient() {
        String trainApiKey = this.secret.cta()
                                        .trainApiKey();

        return TrainClient.builder()
                          .apiKey(trainApiKey)
                          .build();
    }
}
