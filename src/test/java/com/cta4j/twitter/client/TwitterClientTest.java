package com.cta4j.twitter.client;

import com.cta4j.aws.client.SecretsClient;
import com.cta4j.testsupport.TestSecrets;
import com.cta4j.twitter.dto.Media;
import com.cta4j.twitter.dto.Tweet;
import com.cta4j.twitter.exception.TwitterException;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterClientTest {
    private static final String TWEETS_URL = "https://api.x.com/2/tweets";
    private static final String UPLOAD_URL = "https://api.x.com/2/media/upload";
    private static final String METADATA_URL = "https://api.x.com/2/media/metadata";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private SecretsClient secretsClient;

    @Mock
    private OAuth10aService oAuth10aService;

    private TwitterClient client;

    @BeforeEach
    void setUp() {
        this.client = new TwitterClient(this.jsonMapper, this.secretsClient, this.oAuth10aService);
    }

    private void givenSecret() {
        when(this.secretsClient.getSecret()).thenReturn(TestSecrets.secret());
    }

    private void givenResponseBody(String body) throws Exception {
        givenSecret();

        var response = mock(Response.class);

        when(response.isSuccessful()).thenReturn(true);
        when(response.getBody()).thenReturn(body);
        when(this.oAuth10aService.execute(any(OAuthRequest.class))).thenReturn(response);
    }

    private OAuthRequest capturedRequest() throws Exception {
        var captor = ArgumentCaptor.forClass(OAuthRequest.class);

        verify(this.oAuth10aService).execute(captor.capture());

        return captor.getValue();
    }

    private JsonNode capturedPayload() throws Exception {
        return this.jsonMapper.readTree(capturedRequest().getStringPayload());
    }

    @Test
    void postTweetSendsSignedTextOnlyRequest() throws Exception {
        givenResponseBody("""
            {"data": {"id": "t1", "text": "hello", "extra": true}}
            """);

        Tweet tweet = this.client.postTweet("hello");

        assertThat(tweet).isEqualTo(new Tweet("t1", "hello"));

        OAuthRequest request = capturedRequest();

        assertThat(request.getVerb()).isEqualTo(Verb.POST);
        assertThat(request.getUrl()).isEqualTo(TWEETS_URL);
        assertThat(request.getHeaders()).containsEntry("Content-Type", "application/json");

        JsonNode payload = capturedPayload();

        assertThat(payload.get("text").asString()).isEqualTo("hello");
        assertThat(payload.has("media")).isFalse();

        var tokenCaptor = ArgumentCaptor.forClass(OAuth1AccessToken.class);

        verify(this.oAuth10aService).signRequest(tokenCaptor.capture(), any(OAuthRequest.class));

        assertThat(tokenCaptor.getValue().getToken()).isEqualTo(TestSecrets.TWITTER_ACCESS_TOKEN);
        assertThat(tokenCaptor.getValue().getTokenSecret()).isEqualTo(TestSecrets.TWITTER_ACCESS_TOKEN_SECRET);
    }

    @Test
    void postTweetWithMediaIncludesMediaIds() throws Exception {
        givenResponseBody("""
            {"data": {"id": "t1", "text": "hello"}}
            """);

        this.client.postTweet("hello", "m1");

        JsonNode payload = capturedPayload();

        assertThat(payload.get("media").get("media_ids")).hasSize(1);
        assertThat(payload.get("media").get("media_ids").get(0).asString()).isEqualTo("m1");
    }

    @Test
    void postTweetWrapsMalformedResponse() throws Exception {
        givenResponseBody("not json");

        assertThatThrownBy(() -> this.client.postTweet("hello"))
            .isInstanceOf(TwitterException.class)
            .hasMessage("Failed to post tweet");
    }

    @Test
    void postTweetWrapsResponseWithoutData() throws Exception {
        givenResponseBody("""
            {"errors": [{"message": "nope"}]}
            """);

        assertThatThrownBy(() -> this.client.postTweet("hello")).isInstanceOf(TwitterException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429, 500})
    void reportsEndpointAndStatusForUnsuccessfulResponses(int statusCode) throws Exception {
        givenSecret();

        var response = mock(Response.class);

        when(response.isSuccessful()).thenReturn(false);
        when(response.getCode()).thenReturn(statusCode);
        when(this.oAuth10aService.execute(any(OAuthRequest.class))).thenReturn(response);

        assertThatThrownBy(() -> this.client.postTweet("hello"))
            .isInstanceOf(TwitterException.class)
            .hasMessage("Failed to make POST request to %s. Status code: %d".formatted(TWEETS_URL, statusCode));
    }

    @Test
    void doesNotReadTheBodyOfAnUnsuccessfulResponse(@TempDir Path dir) throws Exception {
        givenSecret();

        Path file = Files.write(dir.resolve("map.png"), new byte[] {1});

        var response = mock(Response.class);

        when(response.isSuccessful()).thenReturn(false);
        when(response.getCode()).thenReturn(403);
        when(this.oAuth10aService.execute(any(OAuthRequest.class))).thenReturn(response);

        assertThatThrownBy(() -> this.client.uploadMedia(file.toFile()))
            .isInstanceOf(TwitterException.class)
            .hasMessageContaining(UPLOAD_URL);

        verify(response, never()).getBody();
    }

    @Test
    void postTweetRejectsNullText() {
        assertThatNullPointerException().isThrownBy(() -> this.client.postTweet(null));
    }

    @Test
    void uploadMediaSendsBase64ImageAndParsesMedia(@TempDir Path dir) throws Exception {
        givenResponseBody("""
            {"data": {"id": "m1", "media_key": "3_1"}}
            """);

        byte[] bytes = {1, 2, 3, 4, 5};
        Path file = Files.write(dir.resolve("map.png"), bytes);

        Media media = this.client.uploadMedia(file.toFile());

        assertThat(media).isEqualTo(new Media("m1", "3_1"));
        assertThat(capturedRequest().getUrl()).isEqualTo(UPLOAD_URL);

        JsonNode payload = capturedPayload();

        assertThat(payload.get("media").asString()).isEqualTo(Base64.getEncoder().encodeToString(bytes));
        assertThat(payload.get("media_category").asString()).isEqualTo("tweet_image");
    }

    @Test
    void uploadMediaWrapsUnreadableFileWithoutCallingApi(@TempDir Path dir) throws Exception {
        var missing = dir.resolve("missing.png").toFile();

        assertThatThrownBy(() -> this.client.uploadMedia(missing))
            .isInstanceOf(TwitterException.class)
            .hasMessage("Failed to read file while uploading media");

        verify(this.oAuth10aService, never()).execute(any(OAuthRequest.class));
    }

    @Test
    void uploadMediaWrapsMalformedResponse(@TempDir Path dir) throws Exception {
        givenResponseBody("{}");

        Path file = Files.write(dir.resolve("map.png"), new byte[] {1});

        assertThatThrownBy(() -> this.client.uploadMedia(file.toFile()))
            .isInstanceOf(TwitterException.class)
            .hasMessage("Failed to upload media");
    }

    @Test
    void setMediaAltTextSendsMetadataRequest() throws Exception {
        givenResponseBody("""
            {"data": {"id": "m1"}}
            """);

        this.client.setMediaAltText("m1", "alt text");

        assertThat(capturedRequest().getUrl()).isEqualTo(METADATA_URL);

        JsonNode payload = capturedPayload();

        assertThat(payload.get("id").asString()).isEqualTo("m1");
        assertThat(payload.get("metadata").get("alt_text").get("text").asString()).isEqualTo("alt text");
    }

    @Test
    void setMediaAltTextWrapsMalformedResponse() throws Exception {
        givenResponseBody("garbage");

        assertThatThrownBy(() -> this.client.setMediaAltText("m1", "alt"))
            .isInstanceOf(TwitterException.class)
            .hasMessage("Failed to set alt text");
    }

    @Test
    void setMediaAltTextRejectsNulls() {
        assertThatNullPointerException().isThrownBy(() -> this.client.setMediaAltText(null, "alt"));
        assertThatNullPointerException().isThrownBy(() -> this.client.setMediaAltText("m1", null));
    }

    @Test
    void wrapsTransportFailures() throws Exception {
        givenSecret();

        when(this.oAuth10aService.execute(any(OAuthRequest.class))).thenThrow(new ExecutionException(null));

        assertThatThrownBy(() -> this.client.postTweet("hello"))
            .isInstanceOf(TwitterException.class)
            .hasMessage("Failed to make POST request to " + TWEETS_URL)
            .hasCauseInstanceOf(ExecutionException.class);

        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void wrapsIoFailuresReadingResponseBody() throws Exception {
        givenSecret();

        var response = mock(Response.class);

        when(response.isSuccessful()).thenReturn(true);
        when(response.getBody()).thenThrow(new IOException("closed"));
        when(this.oAuth10aService.execute(any(OAuthRequest.class))).thenReturn(response);

        assertThatThrownBy(() -> this.client.postTweet("hello"))
            .isInstanceOf(TwitterException.class)
            .hasCauseInstanceOf(IOException.class);

        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void restoresInterruptFlagWhenInterrupted() throws Exception {
        givenSecret();

        when(this.oAuth10aService.execute(any(OAuthRequest.class))).thenThrow(new InterruptedException("stop"));

        try {
            assertThatThrownBy(() -> this.client.postTweet("hello"))
                .isInstanceOf(TwitterException.class)
                .hasCauseInstanceOf(InterruptedException.class);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
