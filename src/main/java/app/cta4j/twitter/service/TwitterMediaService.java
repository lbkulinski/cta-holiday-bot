package app.cta4j.twitter.service;

import app.cta4j.common.dto.Response;
import app.cta4j.common.service.SecretService;
import app.cta4j.twitter.dto.TwitterMedia;
import app.cta4j.twitter.dto.UploadMediaResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Service
public final class TwitterMediaService {
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
                .setPath(MEDIA_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            throw new TwitterException("Failed to build URI for media endpoint", e);
        }

        return uri;
    }

    private String buildAuthorizationHeader() {
        String accessToken = this.secretService.getSecret()
                                               .twitter()
                                               .accessToken();

        return String.format("Bearer %s", accessToken);
    }

    private HttpEntity buildEntity(File file) {
        String filename = file.getName();

        return MultipartEntityBuilder.create()
                                     .addBinaryBody("media", file, ContentType.IMAGE_PNG, filename)
                                     .addTextBody("media_category", "tweet_image", ContentType.TEXT_PLAIN)
                                     .addTextBody("media_type", "image/png", ContentType.TEXT_PLAIN)
                                     .build();
    }

    private HttpPost buildRequest(File file) {
        URI uri = this.buildUri();

        HttpPost httpPost = new HttpPost(uri);

        String authorizationHeader = this.buildAuthorizationHeader();

        httpPost.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);

        HttpEntity entity = this.buildEntity(file);

        httpPost.setEntity(entity);

        return httpPost;
    }

    private Response<TwitterMedia> handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        if (statusCode != HttpStatus.SC_OK) {
            return new Response<>(statusCode, null);
        }

        HttpEntity entity = httpResponse.getEntity();

        String entityString = EntityUtils.toString(entity);

        UploadMediaResponse response;

        try {
            response = this.objectMapper.readValue(entityString, UploadMediaResponse.class);
        } catch (JsonProcessingException e) {
            throw new TwitterException("Failed to parse media upload response", e);
        }

        TwitterMedia media = response.data();

        return new Response<>(statusCode, media);
    }

    public TwitterMedia uploadMedia(File file) {
        Objects.requireNonNull(file);

        HttpPost httpPost = this.buildRequest(file);

        Response<TwitterMedia> response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            throw new TwitterException("Failed to execute media upload request", e);
        }

        int statusCode = response.statusCode();

        if (statusCode != HttpStatus.SC_OK) {
            String message = String.format("Failed to upload media, status code: %d", statusCode);

            throw new TwitterException(message);
        }

        TwitterMedia media = response.data();

        if (media == null) {
            throw new TwitterException("Failed to upload media, response body is null");
        }

        return media;
    }
}
