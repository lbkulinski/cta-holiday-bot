package app.cta4j.bluesky.service;

import app.cta4j.bluesky.dto.CreateSessionRequest;
import app.cta4j.bluesky.dto.Session;
import app.cta4j.bluesky.exception.BlueskyException;
import app.cta4j.common.dto.Response;
import app.cta4j.common.dto.Secret;
import app.cta4j.common.service.SecretService;
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

@Service
public final class BlueskySessionService {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "bsky.social";
    private static final String SESSION_ENDPOINT = "/xrpc/com.atproto.server.createSession";

    private final SecretService secretService;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public BlueskySessionService(
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
            uri = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .setPath(SESSION_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            throw new BlueskyException("Failed to build URI for session endpoint", e);
        }

        return uri;
    }

    private HttpEntity buildEntity() {
        Secret.BlueskySecret blueskySecret = this.secretService.getSecret()
                                                               .bluesky();

        CreateSessionRequest request = new CreateSessionRequest(
            blueskySecret.identifier(),
            blueskySecret.appPassword()
        );

        String requestJson;

        try {
            requestJson = this.objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new BlueskyException("Failed to serialize session payload to JSON", e);
        }

        ContentType contentType = ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8);

        return new StringEntity(requestJson, contentType);
    }

    private HttpPost buildRequest() {
        URI uri = this.buildUri();

        HttpPost httpPost = new HttpPost(uri);

        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);

        HttpEntity entity = this.buildEntity();

        httpPost.setEntity(entity);

        return httpPost;
    }

    private Response<Session> handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        HttpEntity entity = httpResponse.getEntity();

        String entityString = EntityUtils.toString(entity);

        if (statusCode != HttpStatus.SC_OK) {
            return new Response<>(statusCode, null);
        }

        Session session;

        try {
            session = this.objectMapper.readValue(entityString, Session.class);
        } catch (JsonProcessingException e) {
            throw new BlueskyException("Failed to parse create session response", e);
        }

        return new Response<>(statusCode, session);
    }

    public Session createSession() {
        HttpPost httpPost = this.buildRequest();

        Response<Session> response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            throw new BlueskyException("Failed to execute create session request", e);
        }

        if (response.statusCode() != HttpStatus.SC_OK) {
            String message = String.format("Failed to create session, status code: %d", response.statusCode());

            throw new BlueskyException(message);
        }

        Session session = response.data();

        if (session == null) {
            throw new BlueskyException("Failed to create session, response body is null");
        }

        return session;
    }
}
