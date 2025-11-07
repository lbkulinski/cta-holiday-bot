package app.cta4j.common.config;

import app.cta4j.common.http.TokenRefreshInterceptor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfiguration {
    @Bean
    public CloseableHttpClient httpClient(TokenRefreshInterceptor interceptor) {
        return HttpClients.custom()
                          .addRequestInterceptorFirst(interceptor)
                          .build();
    }
}
