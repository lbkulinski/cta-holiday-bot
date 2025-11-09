package app.cta4j.common.config;

import app.cta4j.common.service.SecretService;
import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.notifier.config.ConfigBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class RollbarConfiguration {
    private final SecretService secretService;

    @Autowired
    public RollbarConfiguration(SecretService secretService) {
        this.secretService = secretService;
    }

    @Bean
    public Rollbar rollbar(
        @Value("${app.rollbar.environment}") String environment,
        @Value("${app.rollbar.code-version}") String codeVersion
    ) {
        Objects.requireNonNull(environment);

        String accessToken = this.secretService.getSecret()
                                               .rollbar()
                                               .accessToken();

        Config config = ConfigBuilder.withAccessToken(accessToken)
                                     .environment(environment)
                                     .codeVersion(codeVersion)
                                     .build();

        return Rollbar.init(config);
    }
}
