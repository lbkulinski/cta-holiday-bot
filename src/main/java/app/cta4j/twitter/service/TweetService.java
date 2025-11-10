package app.cta4j.twitter.service;

import app.cta4j.common.dto.Response;
import app.cta4j.twitter.exception.TwitterException;
import app.cta4j.common.service.SecretService;
import app.cta4j.twitter.dto.Tweet;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Service
public final class TweetService {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "api.x.com";
    private static final String TWEET_ENDPOINT = "/2/tweets";

    private final SecretService secretService;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public TweetService(
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
                .setPath(TWEET_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            String message = "Failed to build URI for tweet endpoint";

            throw new TwitterException(message, e);
        }

        return uri;
    }

    private String buildAuthorizationHeader() {
        String accessToken = this.secretService.getSecret()
                                               .twitter()
                                               .accessToken();

        return String.format("Bearer %s", accessToken);
    }

    private HttpEntity buildEntity(String text, String mediaId) {
        Map<String, ?> requestMap;

        if (mediaId == null) {
            requestMap = Map.of(
                "text", text
            );
        } else {
            requestMap = Map.of(
                "text", text,
                "media", Map.of(
                    "media_ids", Collections.singletonList(mediaId)
                )
            );
        }

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

        HttpEntity entity = this.buildEntity(text, mediaId);

        httpPost.setEntity(entity);
        
        return httpPost;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CreateTweetResponse(Tweet data) {}

    private Response<Tweet> handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        HttpEntity entity = httpResponse.getEntity();

        String entityString = EntityUtils.toString(entity);

        if (statusCode != HttpStatus.SC_CREATED) {
            return new Response<>(statusCode, null);
        }

        CreateTweetResponse response;

        try {
            response = this.objectMapper.readValue(entityString, CreateTweetResponse.class);
        } catch (JsonProcessingException e) {
            String message = "Failed to parse create tweet response";

            throw new TwitterException(message, e);
        }

        Tweet tweet = response.data();

        return new Response<>(statusCode, tweet);
    }
    
    public Tweet postTweet(String text, String mediaId) {
        Objects.requireNonNull(text);

        HttpPost httpPost = this.buildRequest(text, mediaId);

        Response<Tweet> response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            String message = "Failed to execute create tweet request";

            throw new TwitterException(message, e);
        }

        if (response.statusCode() != HttpStatus.SC_CREATED) {
            String message = String.format("Failed to create tweet, status code: %d", response.statusCode());

            throw new TwitterException(message);
        }

        Tweet tweet = response.data();

        if (tweet == null) {
            String message = "Failed to create tweet, response body is null";

            throw new TwitterException(message);
        }

        return tweet;
    }

    public Tweet postTweet(String text) {
        return this.postTweet(text, null);
    }
}
