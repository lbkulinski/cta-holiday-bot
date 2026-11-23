package com.cta4j.twitter.client;

import com.cta4j.aws.dto.Secret;
import com.cta4j.twitter.dto.CreateMediaMetadataRequest;
import com.cta4j.twitter.dto.CreateMediaMetadataResponse;
import com.cta4j.twitter.dto.CreateTweetRequest;
import com.cta4j.aws.client.SecretsClient;
import com.cta4j.twitter.dto.CreateTweetResponse;
import com.cta4j.twitter.dto.Tweet;
import com.cta4j.twitter.dto.Media;
import com.cta4j.twitter.dto.UploadMediaRequest;
import com.cta4j.twitter.dto.UploadMediaResponse;
import com.cta4j.twitter.exception.TwitterException;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Component
@NullMarked
public final class TwitterClient {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "api.x.com";

    private static final String UPLOAD_MEDIA_ENDPOINT = "/2/media/upload";
    private static final String MEDIA_METADATA_ENDPOINT = "/2/media/metadata";
    private static final String TWEETS_ENDPOINT = "/2/tweets";

    private static final String CREATE_TWEET_URL = UriComponentsBuilder.newInstance()
                                                                       .scheme(SCHEME)
                                                                       .host(HOST_NAME)
                                                                       .path(TWEETS_ENDPOINT)
                                                                       .build()
                                                                       .toUriString();

    private static final String CREATE_MEDIA_METADATA_URL = UriComponentsBuilder.newInstance()
                                                                                .scheme(SCHEME)
                                                                                .host(HOST_NAME)
                                                                                .path(MEDIA_METADATA_ENDPOINT)
                                                                                .build()
                                                                                .toUriString();

    private static final String UPLOAD_MEDIA_URL = UriComponentsBuilder.newInstance()
                                                                       .scheme(SCHEME)
                                                                       .host(HOST_NAME)
                                                                       .path(UPLOAD_MEDIA_ENDPOINT)
                                                                       .build()
                                                                       .toUriString();

    private static final String UPLOAD_MEDIA_CATEGORY = "tweet_image";

    private final JsonMapper jsonMapper;
    private final SecretsClient secretsClient;
    private final OAuth10aService oAuth10aService;

    @Autowired
    public TwitterClient(
        JsonMapper jsonMapper,
        SecretsClient secretsClient,
        OAuth10aService oAuth10aService
    ) {
        this.jsonMapper = jsonMapper;
        this.secretsClient = secretsClient;
        this.oAuth10aService = oAuth10aService;
    }

    public Media uploadMedia(File file) {
        Objects.requireNonNull(file);

        byte[] bytes;

        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new TwitterException("Failed to read file while uploading media", e);
        }

        var requestMedia = Base64.getEncoder().encodeToString(bytes);

        var request = new UploadMediaRequest(requestMedia, UPLOAD_MEDIA_CATEGORY);

        var payload = this.jsonMapper.writeValueAsString(request);

        String body = this.post(UPLOAD_MEDIA_URL, payload);

        try {
            return this.jsonMapper.readValue(body, UploadMediaResponse.class).data();
        } catch (JacksonException e) {
            throw new TwitterException("Failed to upload media", e);
        }
    }

    public void setMediaAltText(String mediaId, String altText) {
        Objects.requireNonNull(mediaId);
        Objects.requireNonNull(altText);

        var alt = new CreateMediaMetadataRequest.AltText(altText);

        var metadata = new CreateMediaMetadataRequest.Metadata(alt);

        var request = new CreateMediaMetadataRequest(mediaId, metadata);

        var payload = this.jsonMapper.writeValueAsString(request);

        String body = this.post(CREATE_MEDIA_METADATA_URL, payload);

        try {
            this.jsonMapper.readValue(body, CreateMediaMetadataResponse.class);
        } catch (JacksonException e) {
            throw new TwitterException("Failed to set alt text", e);
        }
    }
    
    public Tweet postTweet(String text, @Nullable String mediaId) {
        Objects.requireNonNull(text);

        CreateTweetRequest.Media media = null;

        if (mediaId != null) {
            media = new CreateTweetRequest.Media(List.of(mediaId));
        }

        var request = new CreateTweetRequest(text, media);

        var payload = this.jsonMapper.writeValueAsString(request);

        String body = this.post(CREATE_TWEET_URL, payload);

        try {
            return this.jsonMapper.readValue(body, CreateTweetResponse.class).data();
        } catch (JacksonException e) {
            throw new TwitterException("Failed to post tweet", e);
        }
    }

    public Tweet postTweet(String text) {
        return this.postTweet(text, null);
    }

    private String post(String url, String payload) {
        var request = new OAuthRequest(Verb.POST, url);

        request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        request.setPayload(payload);

        Secret.TwitterSecret secret = this.secretsClient.getSecret().twitter();

        var accessToken = new OAuth1AccessToken(secret.accessToken(), secret.accessTokenSecret());

        this.oAuth10aService.signRequest(accessToken, request);

        try (Response response = this.oAuth10aService.execute(request)) {
            if (response.isSuccessful()) {
                return response.getBody();
            }

            int statusCode = response.getCode();

            throw new TwitterException("Failed to make POST request to %s. Status code: %d".formatted(url, statusCode));
        } catch (ExecutionException | IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new TwitterException("Failed to make POST request to %s".formatted(url), e);
        }
    }
}
