package app.cta4j.twitter.service;

import app.cta4j.common.service.SecretService;
import app.cta4j.twitter.dto.Media;
import app.cta4j.twitter.exception.TwitterException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Service
public final class TwitterMediaService {
    private static final Logger log = LoggerFactory.getLogger(TwitterMediaService.class);

    private static final String SCHEME = "https";
    private static final String HOST_NAME = "api.x.com";
    private static final String MEDIA_ENDPOINT = "/2/media/upload";

    private final SecretService secretService;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public TwitterMediaService(
        SecretService secretService,
        CloseableHttpClient httpClient,
        ObjectMapper objectMapper,
        TwitterTokenRefreshService tokenRefreshService
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
                .setPath(MEDIA_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            String message = "Failed to build URI for media endpoint";

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

    private HttpEntity buildEntity(File media) {
        String filename = media.getName();

        return MultipartEntityBuilder.create()
                                     .addBinaryBody("media", media, ContentType.IMAGE_PNG, filename)
                                     .addTextBody("media_category", "tweet_image", ContentType.TEXT_PLAIN)
                                     .addTextBody("media_type", "image/png", ContentType.TEXT_PLAIN)
                                     .build();
    }

    private HttpPost buildRequest(File media) {
        URI uri = this.buildUri();

        HttpPost httpPost = new HttpPost(uri);

        String authorizationHeader = this.buildAuthorizationHeader();

        httpPost.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);

        HttpEntity entity = this.buildEntity(media);

        httpPost.setEntity(entity);

        return httpPost;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponseBody(Media data) {}

    private record Response(int statusCode, ResponseBody body) {}

    private Response handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        HttpEntity entity = httpResponse.getEntity();

        String entityString = EntityUtils.toString(entity);

        if (statusCode != HttpStatus.SC_OK) {
            return new Response(statusCode, null);
        }

        ResponseBody responseBody;

        try {
            responseBody = this.objectMapper.readValue(entityString, ResponseBody.class);
        } catch (JsonProcessingException e) {
            String message = "Failed to parse media upload response";

            throw new TwitterException(message, e);
        }

        return new Response(statusCode, responseBody);
    }

    public Media uploadMedia(File media) {
        Objects.requireNonNull(media);

        HttpPost httpPost = this.buildRequest(media);

        Response response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            String message = "Failed to execute media upload request";

            throw new TwitterException(message, e);
        }

        int statusCode = response.statusCode();

        if (statusCode != HttpStatus.SC_OK) {
            String message = String.format("Failed to upload media, status code: %d", statusCode);

            throw new TwitterException(message);
        }

        ResponseBody body = response.body();

        if (body == null) {
            String message = "Media upload response body is null";

            throw new TwitterException(message);
        }

        return body.data();
    }
}
