package com.cta4j.twitter.service;

import com.cta4j.exception.TwitterServiceException;
import com.cta4j.service.SecretService;
import com.cta4j.twitter.dto.Response;
import com.cta4j.twitter.dto.Tweet;
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
public final class TwitterService {
    private static final Logger log = LoggerFactory.getLogger(TwitterService.class);

    private static final String SCHEME = "https";
    private static final String HOST_NAME = "api.x.com";
    private static final String CREATE_TWEET_ENDPOINT = "/2/tweets";

    private final SecretService secretService;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TokenRefreshService tokenRefreshService;

    @Autowired
    public TwitterService(
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

            throw new TwitterServiceException(message, e);
        }

        return uri;
    }

    private String buildAuthorizationHeader() {
        String accessToken = this.secretService.getSecret()
                                               .twitter()
                                               .getAccessToken();

        return String.format("Bearer %s", accessToken);
    }

    private StringEntity buildStringEntity(String text) {
        Map<String, String> requestMap = Collections.singletonMap("text", text);

        String requestBody;

        try {
            requestBody = this.objectMapper.writeValueAsString(requestMap);
        } catch (JsonProcessingException e) {
            throw new TwitterServiceException("Failed to serialize tweet text to JSON", e);
        }

        ContentType contentType = ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8);

        return new StringEntity(requestBody, contentType);
    }

    private HttpPost buildRequest(String text) {
        URI uri = this.buildUri();

        HttpPost httpPost = new HttpPost(uri);

        String authorizationHeader = this.buildAuthorizationHeader();

        httpPost.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);

        StringEntity stringEntity = this.buildStringEntity(text);

        httpPost.setEntity(stringEntity);
        
        return httpPost;
    }

    private Response handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        if (statusCode != HttpStatus.SC_CREATED) {
            return new Response(statusCode, null);
        }

        HttpEntity entity = httpResponse.getEntity();

        String responseBody = EntityUtils.toString(entity);

        Tweet tweet;

        try {
            tweet = this.objectMapper.readValue(responseBody, Tweet.class);
        } catch (JsonProcessingException e) {
            String message = "Failed to parse create tweet response";

            throw new TwitterServiceException(message, e);
        }

        return new Response(statusCode, tweet);
    }
    
    public Tweet postTweet(String text) {
        Objects.requireNonNull(text);

        HttpPost httpPost = this.buildRequest(text);

        Response response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            String message = "Failed to execute create tweet request";

            throw new TwitterServiceException(message, e);
        }

        int statusCode = response.statusCode();

        if (statusCode == HttpStatus.SC_CREATED) {
            return response.data();
        } else if (statusCode != HttpStatus.SC_UNAUTHORIZED) {
            String message = String.format("Failed to create tweet, status code: %d", response.statusCode());

            throw new TwitterServiceException(message);
        }

        this.tokenRefreshService.refreshAccessToken();

        HttpPost retryHttpPost = this.buildRequest(text);

        try {
            response = this.httpClient.execute(retryHttpPost, this::handleResponse);
        } catch (IOException e) {
            String message = "Failed to execute create tweet request after refreshing access token";

            throw new TwitterServiceException(message, e);
        }

        int retryStatusCode = response.statusCode();

        if (retryStatusCode != HttpStatus.SC_CREATED) {
            String message = String.format("Failed to create tweet after retrying, status code: %d", retryStatusCode);

            throw new TwitterServiceException(message);
        }

        return response.data();
    }
}
