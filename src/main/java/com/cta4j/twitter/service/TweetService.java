package com.cta4j.twitter.service;

import com.cta4j.twitter.exception.TwitterException;
import com.cta4j.secretsmanager.service.SecretService;
import com.cta4j.twitter.dto.Tweet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Service
public final class TweetService {
    private static final Logger log = LoggerFactory.getLogger(TweetService.class);

    private static final String SCHEME = "https";
    private static final String HOST_NAME = "api.x.com";
    private static final String CREATE_TWEET_ENDPOINT = "/2/tweets";

    private final SecretService secretService;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TokenRefreshService tokenRefreshService;

    @Autowired
    public TweetService(
        SecretService secretService,
        CloseableHttpClient httpClient,
        ObjectMapper objectMapper,
        TokenRefreshService tokenRefreshService
    ) {
        this.secretService = secretService;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.tokenRefreshService = tokenRefreshService;
    }

    private URI buildUri() {
        URI uri;

        try {
            uri = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .setPath(CREATE_TWEET_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            String message = "Failed to build URI for create tweet endpoint";

            throw new TwitterException(message, e);
        }

        return uri;
    }

    private String buildAuthorizationHeader() {
        String accessToken = this.secretService.getSecret()
                                               .twitter()
                                               .getAccessToken();

        return String.format("Bearer %s", accessToken);
    }

    private StringEntity buildStringEntity(String text, String mediaId) {
        Map<String, ?> requestMap = Map.of(
            "text", text,
            "media", Map.of(
                "media_ids", Collections.singletonList(mediaId)
            )
        );

        String requestBody;

        try {
            requestBody = this.objectMapper.writeValueAsString(requestMap);
        } catch (JsonProcessingException e) {
            throw new TwitterException("Failed to serialize tweet text to JSON", e);
        }

        ContentType contentType = ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8);

        return new StringEntity(requestBody, contentType);
    }

    private HttpPost buildRequest(String text, String mediaId) {
        URI uri = this.buildUri();

        HttpPost httpPost = new HttpPost(uri);

        String authorizationHeader = this.buildAuthorizationHeader();

        httpPost.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);

        StringEntity stringEntity = this.buildStringEntity(text, mediaId);

        httpPost.setEntity(stringEntity);
        
        return httpPost;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponseBody(Tweet data) {}

    private record Response(int statusCode, ResponseBody body) {}

    private Response handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        HttpEntity entity = httpResponse.getEntity();

        String entityString = EntityUtils.toString(entity);

        ResponseBody body;

        try {
            body = this.objectMapper.readValue(entityString, ResponseBody.class);
        } catch (JsonProcessingException e) {
            String message = "Failed to parse create tweet response";

            throw new TwitterException(message, e);
        }

        return new Response(statusCode, body);
    }
    
    public Tweet postTweet(String text, String mediaId) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(mediaId);

        HttpPost httpPost = this.buildRequest(text, mediaId);

        Response response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            String message = "Failed to execute create tweet request";

            throw new TwitterException(message, e);
        }

        int statusCode = response.statusCode();

        if (statusCode == HttpStatus.SC_CREATED) {
            return response.body.data;
        } else if (statusCode != HttpStatus.SC_UNAUTHORIZED) {
            String message = String.format("Failed to create tweet, status code: %d", statusCode);

            throw new TwitterException(message);
        }

        this.tokenRefreshService.refreshAccessToken();

        HttpPost retryHttpPost = this.buildRequest(text, mediaId);

        Response retryResponse;

        try {
            retryResponse = this.httpClient.execute(retryHttpPost, this::handleResponse);
        } catch (IOException e) {
            String message = "Failed to execute create tweet request after refreshing access token";

            throw new TwitterException(message, e);
        }

        int retryStatusCode = retryResponse.statusCode();

        if (retryStatusCode != HttpStatus.SC_CREATED) {
            String message = String.format("Failed to create tweet after retrying, status code: %d", retryStatusCode);

            throw new TwitterException(message);
        }

        return response.body.data;
    }
}
