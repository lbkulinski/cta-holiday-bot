package app.cta4j.mastodon.service;

import app.cta4j.common.dto.Response;
import app.cta4j.common.service.SecretService;
import app.cta4j.mastodon.dto.MastodonMedia;
import app.cta4j.mastodon.exception.MastodonException;
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
public final class MastodonMediaService {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "mastodon.social";
    private static final String MEDIA_ENDPOINT = "/api/v2/media";

    private final SecretService secretService;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public MastodonMediaService(
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
                                  .setPath(MEDIA_ENDPOINT)
                                  .build();
        } catch (URISyntaxException e) {
            throw new MastodonException("Failed to build URI for media endpoint", e);
        }

        return uri;
    }

    private String buildAuthorizationHeader() {
        String accessToken = this.secretService.getSecret()
                                               .mastodon()
                                               .accessToken();

        return String.format("Bearer %s", accessToken);
    }

    private HttpEntity buildEntity(File file) {
        String filename = file.getName();

        return MultipartEntityBuilder.create()
                                     .addBinaryBody("file", file, ContentType.IMAGE_PNG, filename)
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

    private Response<MastodonMedia> handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        if ((statusCode != HttpStatus.SC_OK) && (statusCode != HttpStatus.SC_ACCEPTED)) {
            return new Response<>(statusCode, null);
        }

        HttpEntity entity = httpResponse.getEntity();

        String entityString = EntityUtils.toString(entity);

        MastodonMedia media;

        try {
            media = this.objectMapper.readValue(entityString, MastodonMedia.class);
        } catch (JsonProcessingException e) {
            throw new MastodonException("Failed to parse media response", e);
        }

        return new Response<>(statusCode, media);
    }

    public MastodonMedia uploadMedia(File file) {
        Objects.requireNonNull(file);

        HttpPost httpPost = this.buildRequest(file);

        Response<MastodonMedia> response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            throw new MastodonException("Failed to execute media upload request", e);
        }

        MastodonMedia media = response.data();

        if (media == null) {
            throw new MastodonException("Failed to upload media, response body is null");
        }

        return media;
    }
}
