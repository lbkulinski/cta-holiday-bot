package app.cta4j.mastodon.service;

import app.cta4j.common.service.SecretService;
import app.cta4j.mastodon.dto.MastodonStatus;
import app.cta4j.mastodon.exception.MastodonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Service
public final class MastodonStatusService {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "mastodon.social";
    private static final String STATUS_ENDPOINT = "/api/v1/statuses";

    private final SecretService secretService;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public MastodonStatusService(
        SecretService secretService,
        CloseableHttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        this.secretService = secretService;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    private URI buildUri() {
        URI uri;

        try {
            uri = new URIBuilder().setScheme(SCHEME)
                                  .setHost(HOST_NAME)
                                  .setPath(STATUS_ENDPOINT)
                                  .build();
        } catch (URISyntaxException e) {
            String message = "Failed to build URI for status endpoint";

            throw new MastodonException(message, e);
        }

        return uri;
    }

    private String buildAuthorizationHeader() {
        String accessToken = this.secretService.getSecret()
                                               .mastodon()
                                               .accessToken();

        return String.format("Bearer %s", accessToken);
    }

    public MastodonStatus postStatus(String text, String mediaId) {
        Objects.requireNonNull(text);

        return null;
    }

    public MastodonStatus postStatus(String text) {
        return this.postStatus(text, null);
    }
}
