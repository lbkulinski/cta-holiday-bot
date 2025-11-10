package app.cta4j.twitter.service;

import app.cta4j.common.dto.Response;
import app.cta4j.twitter.dto.CreateTweetMedia;
import app.cta4j.twitter.dto.CreateTweetRequest;
import app.cta4j.twitter.dto.CreateTweetResponse;
import app.cta4j.twitter.exception.TwitterException;
import app.cta4j.common.service.SecretService;
import app.cta4j.twitter.dto.Tweet;
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
import java.util.List;
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
            throw new TwitterException("Failed to build URI for tweet endpoint", e);
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
        CreateTweetRequest request;

        if (mediaId == null) {
            request = new CreateTweetRequest(text, null);
        } else {
            request = new CreateTweetRequest(
                text,
                new CreateTweetMedia(
                    List.of(mediaId)
                )
            );
        }

        String requestJson;

        try {
            requestJson = this.objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new TwitterException("Failed to serialize request object to JSON", e);
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
            throw new TwitterException("Failed to parse create tweet response", e);
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
            throw new TwitterException("Failed to execute create tweet request", e);
        }

        if (response.statusCode() != HttpStatus.SC_CREATED) {
            String message = String.format("Failed to create tweet, status code: %d", response.statusCode());

            throw new TwitterException(message);
        }

        Tweet tweet = response.data();

        if (tweet == null) {
            throw new TwitterException("Failed to create tweet, response body is null");
        }

        return tweet;
    }

    public Tweet postTweet(String text) {
        return this.postTweet(text, null);
    }
}
