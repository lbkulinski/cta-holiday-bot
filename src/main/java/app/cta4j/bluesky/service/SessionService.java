package app.cta4j.bluesky.service;

import app.cta4j.bluesky.dto.Session;
import app.cta4j.bluesky.exception.BlueskyException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Service
public final class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private static final String SCHEME = "https";
    private static final String HOST_NAME = "bsky.social";
    private static final String SESSION_ENDPOINT = "/xrpc/com.atproto.server.createSession";

    private final Secret secret;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public SessionService(
        SecretService secretService,
        CloseableHttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        this.secret = secretService.getSecret();
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
            String message = "Failed to build URI for session endpoint";

            throw new BlueskyException(message, e);
        }

        return uri;
    }

    private StringEntity buildEntity() {
        Secret.BlueskySecret blueskySecret = this.secret.bluesky();

        Map<String, String> payload = Map.of(
            "identifier", blueskySecret.identifier(),
            "password", blueskySecret.appPassword()
        );

        String jsonPayload;

        try {
            jsonPayload = this.objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            String message = "Failed to serialize session payload to JSON";

            throw new BlueskyException(message, e);
        }

        return new StringEntity(jsonPayload);
    }

    private HttpPost buildRequest() {
        URI uri = this.buildUri();

        HttpPost httpPost = new HttpPost(uri);

        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);

        StringEntity entity = this.buildEntity();

        httpPost.setEntity(entity);

        return httpPost;
    }

    private record Response(int statusCode, Session body) {}

    private Response handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        HttpEntity entity = httpResponse.getEntity();

        String entityString = EntityUtils.toString(entity);

        if (statusCode != HttpStatus.SC_OK) {
            return new Response(statusCode, null);
        }

        Session body;

        try {
            body = this.objectMapper.readValue(entityString, Session.class);
        } catch (JsonProcessingException e) {
            String message = "Failed to parse create session response";

            throw new BlueskyException(message, e);
        }

        return new Response(statusCode, body);
    }

    public Session createSession() {
        HttpPost httpPost = this.buildRequest();

        Response response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            String message = "Failed to execute create session request";

            throw new BlueskyException(message, e);
        }

        if (response.statusCode() != HttpStatus.SC_OK) {
            String message = String.format("Failed to create session, status code: %d", response.statusCode());

            throw new BlueskyException(message);
        }

        return response.body();
    }
}
