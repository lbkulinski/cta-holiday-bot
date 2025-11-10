package app.cta4j.bluesky.service;

import app.cta4j.bluesky.dto.*;
import app.cta4j.bluesky.exception.BlueskyException;
import app.cta4j.common.dto.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public final class BlueskyRecordService {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "bsky.social";
    private static final String RECORD_ENDPOINT = "/xrpc/com.atproto.repo.createRecord";
    private static final String RECORD_COLLECTION = "app.bsky.feed.post";
    private static final String EMBED_TYPE = "app.bsky.embed.images";
    private static final String IMAGE_ALT = "CTA Holiday Train on the tracks";

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public BlueskyRecordService(CloseableHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    private URI buildUri() {
        URI uri;

        try {
            uri = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .setPath(RECORD_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            String message = "Failed to build URI for record endpoint";

            throw new BlueskyException(message, e);
        }

        return uri;
    }

    private String buildAuthorizationHeader(Session session) {
        String accessJwt = session.accessJwt();

        return String.format("Bearer %s", accessJwt);
    }

    private HttpEntity buildEntity(Session session, String text, BlueskyBlob blob) {
        String handle = session.handle();

        CreateRecordData data;

        if (blob == null) {
            data = new CreateRecordData(
                text,
                Instant.now(),
                null
            );
        } else {
            data = new CreateRecordData(
                text,
                Instant.now(),
                new Embed(
                    EMBED_TYPE,
                    List.of(
                        new Image(IMAGE_ALT, blob)
                    )
                )
            );
        }

        CreateRecordRequest request = new CreateRecordRequest(handle, RECORD_COLLECTION, data);

        String requestJson;

        try {
            requestJson = this.objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new BlueskyException("Failed to serialize record text to JSON", e);
        }

        ContentType contentType = ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8);

        return new StringEntity(requestJson, contentType);
    }

    private HttpPost buildRequest(Session session, String text, BlueskyBlob blob) {
        URI uri = this.buildUri();

        HttpPost httpPost = new HttpPost(uri);

        String authorizationHeader = this.buildAuthorizationHeader(session);

        httpPost.setHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);

        HttpEntity entity = this.buildEntity(session, text, blob);

        httpPost.setEntity(entity);

        return httpPost;
    }

    private Response<BlueskyRecord> handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        HttpEntity entity = httpResponse.getEntity();

        String entityString = EntityUtils.toString(entity);

        if (statusCode != HttpStatus.SC_OK) {
            return new Response<>(statusCode, null);
        }

        BlueskyRecord record;

        try {
            record = this.objectMapper.readValue(entityString, BlueskyRecord.class);
        } catch (JsonProcessingException e) {
            String message = "Failed to parse create record response";

            throw new BlueskyException(message, e);
        }

        return new Response<>(statusCode, record);
    }

    public BlueskyRecord createRecord(Session session, String text, BlueskyBlob blob) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(text);

        HttpPost httpPost = this.buildRequest(session, text, blob);

        Response<BlueskyRecord> response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            String message = "Failed to execute create record request";

            throw new BlueskyException(message, e);
        }

        if (response.statusCode() != HttpStatus.SC_OK) {
            String message = String.format("Failed to create record, status code: %d", response.statusCode());

            throw new BlueskyException(message);
        }

        BlueskyRecord record = response.data();

        if (record == null) {
            String message = "Failed to create record, response body is null";

            throw new BlueskyException(message);
        }

        return record;
    }

    public BlueskyRecord createRecord(Session session, String text) {
        return this.createRecord(session, text, null);
    }
}
