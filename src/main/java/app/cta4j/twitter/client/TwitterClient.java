package app.cta4j.twitter.client;

import app.cta4j.aws.dto.Secret;
import app.cta4j.twitter.dto.CreateTweetMedia;
import app.cta4j.twitter.dto.CreateTweetRequest;
import app.cta4j.aws.client.SecretsClient;
import app.cta4j.twitter.dto.CreateTweetResponse;
import app.cta4j.twitter.dto.Tweet;
import app.cta4j.twitter.dto.TwitterMedia;
import app.cta4j.twitter.dto.UploadMediaRequest;
import app.cta4j.twitter.dto.UploadMediaResponse;
import app.cta4j.twitter.exception.TwitterException;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Service
@NullMarked
public final class TwitterClient {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "api.x.com";

    private static final String TWEETS_ENDPOINT = "/2/tweets";
    private static final String UPLOAD_MEDIA_ENDPOINT = "/2/media/upload";

    private static final String CREATE_TWEET_URL;
    private static final String UPLOAD_MEDIA_URL;

    private static final String UPLOAD_MEDIA_CATEGORY = "tweet_image";

    static {
        try {
            CREATE_TWEET_URL = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .appendPath(TWEETS_ENDPOINT)
                .build()
                .toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to build URI for tweet endpoint", e);
        }

        try {
            UPLOAD_MEDIA_URL = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .appendPath(UPLOAD_MEDIA_ENDPOINT)
                .build()
                .toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to build URI for upload media endpoint", e);
        }
    }

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
    
    public Tweet postTweet(String text, @Nullable String mediaId) {
        Objects.requireNonNull(text);

        var request = new OAuthRequest(Verb.POST, CREATE_TWEET_URL);

        String payload = this.getCreateTweetPayload(text, mediaId);

        request.setPayload(payload);

        Secret.TwitterSecret secret = this.secretsClient.getSecret().twitter();

        var accessToken = new OAuth1AccessToken(secret.accessToken(), secret.accessTokenSecret());

        this.oAuth10aService.signRequest(accessToken, request);

        try (Response response = this.oAuth10aService.execute(request)) {
            String body = response.getBody();

            return this.jsonMapper.readValue(body, CreateTweetResponse.class).data();
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new TwitterException("Failed to post tweet", e);
        }
    }

    public Tweet postTweet(String text) {
        return this.postTweet(text, null);
    }

    public TwitterMedia uploadMedia(File file) {
        Objects.requireNonNull(file);

        var request = new OAuthRequest(Verb.POST, UPLOAD_MEDIA_URL);

        String payload = this.getUploadMediaPayload(file);

        request.setPayload(payload);

        Secret.TwitterSecret secret = this.secretsClient.getSecret().twitter();

        var accessToken = new OAuth1AccessToken(secret.accessToken(), secret.accessTokenSecret());

        this.oAuth10aService.signRequest(accessToken, request);

        try (Response response = this.oAuth10aService.execute(request)) {
            String body = response.getBody();

            return this.jsonMapper.readValue(body, UploadMediaResponse.class).data();
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new TwitterException("Failed to upload media", e);
        }
    }

    private String getCreateTweetPayload(String text, @Nullable String mediaId) {
        CreateTweetMedia media = null;

        if (mediaId != null) {
            media = new CreateTweetMedia(List.of(mediaId));
        }

        var request = new CreateTweetRequest(text, media);

        return this.jsonMapper.writeValueAsString(request);
    }

    private String getUploadMediaPayload(File file) {
        byte[] bytes;

        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new TwitterException("Failed to read file while uploading media", e);
        }

        String media = Base64.getEncoder().encodeToString(bytes);

        var request = new UploadMediaRequest(media, UPLOAD_MEDIA_CATEGORY);

        return this.jsonMapper.writeValueAsString(request);
    }
}
