package app.cta4j.bluesky.service;

import app.cta4j.bluesky.dto.Record;
import app.cta4j.bluesky.dto.Session;
import app.cta4j.bluesky.exception.BlueskyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Service
public final class BlueskyRecordService {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "bsky.social";
    private static final String RECORD_ENDPOINT = "/xrpc/com.atproto.repo.uploadBlob";

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public BlueskyRecordService(CloseableHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public Record createRecord(Session session, String text, String mediaId) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(text);
        Objects.requireNonNull(mediaId);

        URI uri;

        try {
            uri = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .setPath(RECORD_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            String message = "Failed to build URI for create record endpoint";

            throw new BlueskyException(message, e);
        }

        String accessJwt = session.accessJwt();

        String authHeaderString = String.format("Bearer %s", accessJwt);

        Header authHeader = new BasicHeader(HttpHeaders.AUTHORIZATION, authHeaderString);

        return null;
    }
}
