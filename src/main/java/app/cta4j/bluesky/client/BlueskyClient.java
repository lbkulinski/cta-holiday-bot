package app.cta4j.bluesky.client;

import app.cta4j.bluesky.dto.Blob;
import app.cta4j.bluesky.dto.CreateRecordRequest;
import app.cta4j.bluesky.dto.ImageBlob;
import app.cta4j.bluesky.dto.Record;
import app.cta4j.bluesky.dto.Session;
import app.cta4j.bluesky.dto.CreateSessionRequest;
import app.cta4j.bluesky.dto.UploadBlobResponse;
import app.cta4j.bluesky.exception.BlueskyException;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
@NullMarked
public final class BlueskyClient {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "bsky.social";

    private static final String CREATE_SESSION_ENDPOINT = "/xrpc/com.atproto.server.createSession";
    private static final String UPLOAD_BLOB_ENDPOINT = "/xrpc/com.atproto.repo.uploadBlob";
    private static final String CREATE_RECORD_ENDPOINT = "/xrpc/com.atproto.repo.createRecord";

    private static final URI CREATE_SESSION_URI;
    private static final URI UPLOAD_BLOB_URI;
    private static final URI CREATE_RECORD_URI;

    private static final String AUTH_HEADER_TEMPLATE = "Bearer %s";

    private static final String EMBED_TYPE = "app.bsky.embed.images";
    private static final String RECORD_TYPE = "app.bsky.feed.post";

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

        try {
            CREATE_RECORD_URI = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .appendPath(CREATE_RECORD_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to build URI for create record endpoint", e);
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

        var authHeader = AUTH_HEADER_TEMPLATE.formatted(session.accessJwt());
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

    public Record createRecord(Session session, String text, @Nullable ImageBlob image) {
        Objects.requireNonNull(text);

        var authHeader = AUTH_HEADER_TEMPLATE.formatted(session.accessJwt());

        CreateRecordRequest.Embed embed = null;

        if (image != null) {
            embed = new CreateRecordRequest.Embed(EMBED_TYPE, List.of(image));
        }

        var requestRecord = new CreateRecordRequest.Record(RECORD_TYPE, text, Instant.now(), embed);

        var request = new CreateRecordRequest(session.did(), RECORD_TYPE, requestRecord);

        Record record;

        try {
            record = this.restClient.post()
                                    .uri(CREATE_RECORD_URI)
                                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(request)
                                    .retrieve()
                                    .toEntity(Record.class)
                                    .getBody();
        } catch (RestClientException e) {
            throw new BlueskyException("Failed to create record", e);
        }

        if (record == null) {
            throw new BlueskyException("Failed to create record");
        }

        return record;
    }

    public Record createRecord(Session session, String text) {
        return this.createRecord(session, text, null);
    }
}
