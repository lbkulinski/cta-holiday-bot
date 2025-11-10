package app.cta4j.mastodon.service;

import app.cta4j.common.dto.Response;
import app.cta4j.common.service.SecretService;
import app.cta4j.mastodon.dto.CreateStatusRequest;
import app.cta4j.mastodon.dto.MastodonStatus;
import app.cta4j.mastodon.exception.MastodonException;
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
import java.util.Arrays;
import java.util.List;
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

    private HttpEntity buildEntity(String text, String mediaId) {
        CreateStatusRequest request;

        if (mediaId == null) {
            request = new CreateStatusRequest(text, null);
        } else {
            List<String> mediaIds = List.of(mediaId);

            request = new CreateStatusRequest(text, mediaIds);
        }

        String requestJson;

        try {
            requestJson = this.objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new MastodonException("Failed to serialize request object to JSON", e);
        }

        ContentType contentType = ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8);

        return new StringEntity(requestJson, contentType);
    }

    private HttpPost buildRequest(String text, String mediaId) {
        URI uri = this.buildUri();

        HttpPost httpPost = new HttpPost(uri);

        String authorizationHeader = this.buildAuthorizationHeader();

        httpPost.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);

        HttpEntity entity = this.buildEntity(text, mediaId);

        httpPost.setEntity(entity);

        return httpPost;
    }

    private Response<MastodonStatus> handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        if (statusCode != HttpStatus.SC_OK) {
            return new Response<>(statusCode, null);
        }

        HttpEntity entity = httpResponse.getEntity();

        String entityString = EntityUtils.toString(entity);

        MastodonStatus status;

        try {
            status = this.objectMapper.readValue(entityString, MastodonStatus.class);
        }  catch (JsonProcessingException e) {
            throw new MastodonException("Failed to parse status response", e);
        }

        return new Response<>(statusCode, status);
    }

    public MastodonStatus postStatus(String text, String mediaId) {
        Objects.requireNonNull(text);

        HttpPost httpPost = this.buildRequest(text, mediaId);

        Response<MastodonStatus> response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            throw new MastodonException("Failed to execute media upload request", e);
        }

        MastodonStatus status = response.data();

        if (status == null) {
            throw new MastodonException("Failed to create status, response body is null");
        }

        return status;
    }

    public MastodonStatus postStatus(String text) {
        return this.postStatus(text, null);
    }
}
