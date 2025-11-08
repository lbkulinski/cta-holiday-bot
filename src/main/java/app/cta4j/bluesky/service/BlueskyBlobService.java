package app.cta4j.bluesky.service;

import app.cta4j.bluesky.dto.Blob;
import app.cta4j.bluesky.dto.Session;
import app.cta4j.bluesky.dto.UploadBlobResponse;
import app.cta4j.bluesky.exception.BlueskyException;
import app.cta4j.common.dto.Response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Service
public final class BlueskyBlobService {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "bsky.social";
    private static final String BLOB_ENDPOINT = "/xrpc/com.atproto.repo.uploadBlob";

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public BlueskyBlobService(CloseableHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    private URI getUri() {
        URI uri;

        try {
            uri = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .setPath(BLOB_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            String message = "Failed to build URI for blob endpoint";

            throw new BlueskyException(message, e);
        }

        return uri;
    }

    private Header getAuthorizationHeader(Session session) {
        String accessJwt = session.accessJwt();

        String headerString = String.format("Bearer %s", accessJwt);

        return new BasicHeader(HttpHeaders.AUTHORIZATION, headerString);
    }

    private HttpPost buildRequest(Session session, File file) {
        URI uri = this.getUri();

        HttpPost httpPost = new HttpPost(uri);

        Header authorizationHeader = this.getAuthorizationHeader(session);

        httpPost.addHeader(authorizationHeader);

        FileEntity fileEntity = new FileEntity(file, ContentType.IMAGE_PNG);

        httpPost.setEntity(fileEntity);

        return httpPost;
    }

    private Response<Blob> handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        if (statusCode != HttpStatus.SC_OK) {
            return new  Response<>(statusCode, null);
        }

        HttpEntity entity = httpResponse.getEntity();

        String entityString = EntityUtils.toString(entity);

        UploadBlobResponse uploadBlobResponse;

        try {
            uploadBlobResponse = this.objectMapper.readValue(entityString, UploadBlobResponse.class);
        } catch (JsonProcessingException e) {
            String message = "Failed to parse upload blob response";

            throw new BlueskyException(message, e);
        }

        Blob blob = uploadBlobResponse.blob();

        return new Response<>(statusCode, blob);
    }

    public Blob uploadBlob(Session session, File file) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(file);

        HttpPost httpPost = this.buildRequest(session, file);

        Response<Blob> response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            String message = "Failed to execute blob upload request";

            throw new BlueskyException(message, e);
        }

        int statusCode = response.statusCode();

        if (statusCode != HttpStatus.SC_OK) {
            String message = String.format("Failed to upload blob, status code: %d", statusCode);

            throw new BlueskyException(message);
        }

        Blob blob = response.data();

        if (blob == null) {
            String message = "Failed to upload blob, response body is null";

            throw new BlueskyException(message);
        }

        return blob;
    }
}
