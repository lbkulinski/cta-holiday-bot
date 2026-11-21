package com.cta4j.bluesky.client;

import com.cta4j.bluesky.dto.Blob;
import com.cta4j.bluesky.dto.CreateRecordRequest;
import com.cta4j.bluesky.dto.ImageBlob;
import com.cta4j.bluesky.dto.Record;
import com.cta4j.bluesky.dto.Session;
import com.cta4j.bluesky.dto.CreateSessionRequest;
import com.cta4j.bluesky.dto.UploadBlobResponse;
import com.cta4j.bluesky.exception.BlueskyException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
@NullMarked
public final class BlueskyClient {
    private static final String SCHEME = "https";
    private static final String HOST = "bsky.social";

    private static final String CREATE_SESSION_ENDPOINT = "/xrpc/com.atproto.server.createSession";
    private static final String UPLOAD_BLOB_ENDPOINT = "/xrpc/com.atproto.repo.uploadBlob";
    private static final String CREATE_RECORD_ENDPOINT = "/xrpc/com.atproto.repo.createRecord";

    private static final URI CREATE_SESSION_URI = UriComponentsBuilder.newInstance()
                                                                      .scheme(SCHEME)
                                                                      .host(HOST)
                                                                      .path(CREATE_SESSION_ENDPOINT)
                                                                      .build()
                                                                      .toUri();

    private static final URI UPLOAD_BLOB_URI = UriComponentsBuilder.newInstance()
                                                                   .scheme(SCHEME)
                                                                   .host(HOST)
                                                                   .path(UPLOAD_BLOB_ENDPOINT)
                                                                   .build()
                                                                   .toUri();

    private static final URI CREATE_RECORD_URI = UriComponentsBuilder.newInstance()
                                                                     .scheme(SCHEME)
                                                                     .host(HOST)
                                                                     .path(CREATE_RECORD_ENDPOINT)
                                                                     .build()
                                                                     .toUri();

    private static final String AUTH_HEADER_TEMPLATE = "Bearer %s";

    private static final String EMBED_TYPE = "app.bsky.embed.images";
    private static final String RECORD_TYPE = "app.bsky.feed.post";

    private final RestClient restClient;

    @Autowired
    public BlueskyClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public Session createSession(String identifier, String password) {
        Objects.requireNonNull(identifier);
        Objects.requireNonNull(password);

        var request = new CreateSessionRequest(identifier, password);

        try {
            return this.restClient.post()
                                  .uri(CREATE_SESSION_URI)
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body(request)
                                  .retrieve()
                                  .requiredBody(Session.class);
        } catch (RestClientException | IllegalStateException e) {
            throw new BlueskyException("Failed to create session", e);
        }
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

        try {
            return this.restClient.post()
                                  .uri(UPLOAD_BLOB_URI)
                                  .header(HttpHeaders.AUTHORIZATION, authHeader)
                                  .header(HttpHeaders.CONTENT_LENGTH, lengthHeader)
                                  .contentType(MediaType.IMAGE_PNG)
                                  .body(bytes)
                                  .retrieve()
                                  .requiredBody(UploadBlobResponse.class)
                                  .blob();
        } catch (RestClientException | IllegalStateException e) {
            throw new BlueskyException("Failed to upload blob", e);
        }
    }

    public Record createRecord(Session session, String text, @Nullable ImageBlob image) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(text);

        var authHeader = AUTH_HEADER_TEMPLATE.formatted(session.accessJwt());

        CreateRecordRequest.Embed embed = null;

        if (image != null) {
            embed = new CreateRecordRequest.Embed(EMBED_TYPE, List.of(image));
        }

        var requestRecord = new CreateRecordRequest.Record(RECORD_TYPE, text, Instant.now(), embed);

        var request = new CreateRecordRequest(session.did(), RECORD_TYPE, requestRecord);

        try {
            return this.restClient.post()
                                  .uri(CREATE_RECORD_URI)
                                  .header(HttpHeaders.AUTHORIZATION, authHeader)
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body(request)
                                  .retrieve()
                                  .requiredBody(Record.class);
        } catch (RestClientException | IllegalStateException e) {
            throw new BlueskyException("Failed to create record", e);
        }
    }

    public Record createRecord(Session session, String text) {
        return this.createRecord(session, text, null);
    }
}
