package app.cta4j.common.http;

import app.cta4j.common.service.SecretService;
import app.cta4j.twitter.service.TwitterTokenRefreshService;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Component
public final class TokenRefreshInterceptor implements HttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(TokenRefreshInterceptor.class);

    private static final String TWITTER_HOST_NAME = "api.x.com";
    private static final Duration REFRESH_THRESHOLD = Duration.ofMinutes(5);

    private final SecretService secretService;
    private final TwitterTokenRefreshService tokenRefreshService;

    private final ReentrantLock lock;

    @Autowired
    public TokenRefreshInterceptor(SecretService secretService, TwitterTokenRefreshService tokenRefreshService) {
        this.secretService = secretService;
        this.tokenRefreshService = tokenRefreshService;
        this.lock = new ReentrantLock();
    }

    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) {
        URI uri;

        try {
            uri = httpRequest.getUri();
        } catch (URISyntaxException e) {
            String message = "Failed to get URI from HTTP request";

            log.error(message, e);

            return;
        }

        String host = uri.getHost();

        if (!Objects.equals(host, TWITTER_HOST_NAME)) {
            return;
        }

        this.lock.lock();

        try {
            Instant now = Instant.now();

            Instant expirationTime = this.secretService.getSecret()
                                                       .twitter()
                                                       .expirationTime();

            Instant thresholdTime = expirationTime.minus(REFRESH_THRESHOLD);

            if (!now.isAfter(thresholdTime)) {
                return;
            }

            String newAccessToken = this.tokenRefreshService.refreshAccessToken();

            String newAuthorizationHeader = String.format("Bearer %s", newAccessToken);

            httpRequest.setHeader(HttpHeaders.AUTHORIZATION, newAuthorizationHeader);
        } finally {
            this.lock.unlock();
        }
    }
}
