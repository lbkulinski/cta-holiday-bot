package app.cta4j.twitter.client;

import app.cta4j.aws.dto.Secret;
import app.cta4j.twitter.dto.CreateMediaMetadataRequest;
import app.cta4j.twitter.dto.CreateMediaMetadataResponse;
import app.cta4j.twitter.dto.CreateTweetMedia;
import app.cta4j.twitter.dto.CreateTweetRequest;
import app.cta4j.aws.client.SecretsClient;
import app.cta4j.twitter.dto.CreateTweetResponse;
import app.cta4j.twitter.dto.MediaMetadata;
import app.cta4j.twitter.dto.Tweet;
import app.cta4j.twitter.dto.Media;
import app.cta4j.twitter.dto.UploadMediaRequest;
import app.cta4j.twitter.dto.UploadMediaResponse;
import app.cta4j.twitter.exception.TwitterException;
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

        String requestMedia = Base64.getEncoder().encodeToString(bytes);

        var request = new UploadMediaRequest(requestMedia, UPLOAD_MEDIA_CATEGORY);

        String payload = this.jsonMapper.writeValueAsString(request);

        String body = this.post(UPLOAD_MEDIA_URL, payload);

        UploadMediaResponse response;

        try {
            response = this.jsonMapper.readValue(body, UploadMediaResponse.class);
        } catch (JacksonException e) {
            throw new TwitterException("Failed to deserialize upload media response", e);
        }

        Media media = response.data();

        if (media == null) {
            throw new TwitterException("Failed to upload media");
        }

        return media;
    }

    public void setMediaAltText(String mediaId, String altText) {
        Objects.requireNonNull(mediaId);
        Objects.requireNonNull(altText);

        var alt = new CreateMediaMetadataRequest.AltText(altText);

        var metadata = new CreateMediaMetadataRequest.Metadata(alt);

        var request = new CreateMediaMetadataRequest(mediaId, metadata);

        String payload = this.jsonMapper.writeValueAsString(request);

        String body = this.post(CREATE_MEDIA_METADATA_URL, payload);

        CreateMediaMetadataResponse response;

        try {
            response = this.jsonMapper.readValue(body, CreateMediaMetadataResponse.class);
        } catch (JacksonException e) {
            throw new TwitterException("Failed to deserialize set altText text response", e);
        }

        MediaMetadata mediaMetadata = response.data();

        if (mediaMetadata == null) {
            throw new TwitterException("Failed to set altText text");
        }
    }
    
    public Tweet postTweet(String text, @Nullable String mediaId) {
        Objects.requireNonNull(text);

        CreateTweetMedia media = null;

        if (mediaId != null) {
            media = new CreateTweetMedia(List.of(mediaId));
        }

        var request = new CreateTweetRequest(text, media);

        String payload = this.jsonMapper.writeValueAsString(request);

        String body = this.post(CREATE_TWEET_URL, payload);

        CreateTweetResponse response;

        try {
            response = this.jsonMapper.readValue(body, CreateTweetResponse.class);
        } catch (JacksonException e) {
            throw new TwitterException("Failed to deserialize post tweet response", e);
        }

        Tweet data = response.data();

        if (data == null) {
            throw new TwitterException("Failed to post tweet");
        }

        return data;
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
            return response.getBody();
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new TwitterException("Failed to make POST request", e);
        }
    }
}
