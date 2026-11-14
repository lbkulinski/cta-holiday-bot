package app.cta4j.bluesky.client;

import app.cta4j.bluesky.dto.BlueskySession;
import app.cta4j.bluesky.dto.CreateBlueskySessionRequest;
import app.cta4j.bluesky.exception.BlueskyException;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Component
@NullMarked
public final class BlueskyClient {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "bsky.social";

    private static final String CREATE_SESSION_ENDPOINT = "/xrpc/com.atproto.server.createSession";
    private static final String UPLOAD_BLOB_ENDPOINT = "/xrpc/com.atproto.repo.uploadBlob";

    private static final URI CREATE_SESSION_URI;
    private static final URI UPLOAD_BLOB_URI;

    static {
        try {
            CREATE_SESSION_URI = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .appendPath(CREATE_SESSION_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to build URI for create session endpoint", e);
        }

        try {
            UPLOAD_BLOB_URI = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .appendPath(UPLOAD_BLOB_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to build URI for upload blob endpoint", e);
        }
    }

    private final RestClient restClient;

    @Autowired
    public BlueskyClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public BlueskySession createSession(String identifier, String password) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(password);

        var request = new CreateBlueskySessionRequest(identifier, password);

        BlueskySession session;

        try {
            session = this.restClient.post()
                                     .uri(CREATE_SESSION_URI)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .body(request)
                                     .retrieve()
                                     .toEntity(BlueskySession.class)
                                     .getBody();
        } catch (RestClientException e) {
            throw new BlueskyException("Failed to create session", e);
        }

        if (session == null) {
            throw new BlueskyException("Failed to create session");
        }

        return session;
    }
}
