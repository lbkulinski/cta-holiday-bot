package app.cta4j.bluesky.client;

import app.cta4j.bluesky.dto.Blob;
import app.cta4j.bluesky.dto.Session;
import app.cta4j.bluesky.dto.CreateSessionRequest;
import app.cta4j.bluesky.dto.UploadBlobResponse;
import app.cta4j.bluesky.exception.BlueskyException;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
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

    private static final String AUTH_HEADER_TEMPLATE = "Bearer %s";

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

    public Session createSession(String identifier, String password) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(password);

        var request = new CreateSessionRequest(identifier, password);

        Session session;

        try {
            session = this.restClient.post()
                                     .uri(CREATE_SESSION_URI)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .body(request)
                                     .retrieve()
                                     .toEntity(Session.class)
                                     .getBody();
        } catch (RestClientException e) {
            throw new BlueskyException("Failed to create session", e);
        }

        if (session == null) {
            throw new BlueskyException("Failed to create session");
        }

        return session;
    }

    public Blob uploadBlob(Session session, File file) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(file);

        String authHeader = AUTH_HEADER_TEMPLATE.formatted(session.accessJwt());
        var lengthHeader = String.valueOf(file.length());

        byte[] bytes;

        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new BlueskyException("Failed to read file while uploading blob", e);
        }

        UploadBlobResponse response;

        try {
            response = this.restClient.post()
                                      .uri(UPLOAD_BLOB_URI)
                                      .header(HttpHeaders.AUTHORIZATION, authHeader)
                                      .header(HttpHeaders.CONTENT_LENGTH, lengthHeader)
                                      .contentType(MediaType.IMAGE_PNG)
                                      .body(bytes)
                                      .retrieve()
                                      .toEntity(UploadBlobResponse.class)
                                      .getBody();
        } catch (RestClientException e) {
            throw new BlueskyException("Failed to upload blob", e);
        }

        if (response == null || response.blob() == null) {
            throw new BlueskyException("Failed to upload blob");
        }

        return response.blob();
    }
}
