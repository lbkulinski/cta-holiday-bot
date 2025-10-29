package com.cta4j.twitter.service;

import com.cta4j.dto.Secret;
import com.cta4j.exception.TwitterServiceException;
import com.cta4j.service.SecretService;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public final class TokenRefreshService {
    private static final Logger log = LoggerFactory.getLogger(TokenRefreshService.class);

    private static final String SCHEME = "https";
    private static final String HOST_NAME = "api.x.com";
    private static final String OAUTH_TOKEN_ENDPOINT = "/2/oauth2/token";

    private final SecretService secretService;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public TokenRefreshService(
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
                .setPath(OAUTH_TOKEN_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            String message = "Failed to build URI for token refresh endpoint";

            throw new TwitterServiceException(message, e);
        }

        return uri;
    }

    private String buildAuthorizationHeader() {
        Secret.TwitterSecret secret = this.secretService.getSecret()
                                                        .twitter();

        String clientId = secret.clientId();

        String clientSecret = secret.clientSecret();

        String credentials = String.format("%s:%s", clientId, clientSecret);

        byte[] bytes = credentials.getBytes(StandardCharsets.UTF_8);

        String encodedCredentials = Base64.getEncoder()
                                          .encodeToString(bytes);

        return String.format("Basic %s", encodedCredentials);
    }

    private StringEntity buildStringEntity(String refreshToken) {
        String encodedToken = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        String requestBody = String.format("grant_type=refresh_token&refresh_token=%s", encodedToken);

        ContentType contentType = ContentType.APPLICATION_FORM_URLENCODED.withCharset(StandardCharsets.UTF_8);

        return new StringEntity(requestBody, contentType);
    }

    private HttpPost buildRequest(String refreshToken) {
        URI uri = this.buildUri();

        HttpPost httpPost = new HttpPost(uri);

        String authorizationHeader = this.buildAuthorizationHeader();

        httpPost.addHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON);

        StringEntity stringEntity = this.buildStringEntity(refreshToken);

        httpPost.setEntity(stringEntity);

        return httpPost;
    }

    private record Response(String accessToken, String refreshToken) {}

    private Response handleResponse(ClassicHttpResponse httpResponse) throws IOException, ParseException {
        int statusCode = httpResponse.getCode();

        if (statusCode != HttpStatus.SC_OK) {
            log.info("HTTP status code {}, reason {}", statusCode, httpResponse.getReasonPhrase());

            return null;
        }

        HttpEntity entity = httpResponse.getEntity();

        String responseBody = EntityUtils.toString(entity);

        Response response;

        try {
            response = this.objectMapper.readValue(responseBody, Response.class);
        } catch (JsonProcessingException e) {
            String message = "Failed to parse create tweet response";

            throw new TwitterServiceException(message, e);
        }

        return response;
    }

    public String refreshAccessToken() {
        String refreshToken = this.secretService.getSecret()
                                                .twitter()
                                                .refreshToken();

        HttpPost httpPost = this.buildRequest(refreshToken);

        Response response;

        try {
            response = this.httpClient.execute(httpPost, this::handleResponse);
        } catch (IOException e) {
            String message = "Failed to execute token refresh request";

            throw new TwitterServiceException(message, e);
        }

        if (response == null) {
            String message = "Token refresh request was not successful";

            throw new TwitterServiceException(message);
        }

        this.secretService.setTwitterTokens(response.accessToken,  response.refreshToken);

        return response.accessToken;
    }
}
