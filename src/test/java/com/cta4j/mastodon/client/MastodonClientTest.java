package com.cta4j.mastodon.client;

import com.cta4j.aws.client.SecretsClient;
import com.cta4j.mastodon.dto.Media;
import com.cta4j.mastodon.dto.Status;
import com.cta4j.mastodon.exception.MastodonException;
import com.cta4j.testsupport.TestSecrets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class MastodonClientTest {
    private static final String BEARER = "Bearer " + TestSecrets.MASTODON_TOKEN;

    @Mock
    private SecretsClient secretsClient;

    private MockRestServiceServer server;
    private MastodonClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        this.server = MockRestServiceServer.bindTo(builder).build();
        this.client = new MastodonClient(this.secretsClient, builder);
    }

    private void givenSecret() {
        when(this.secretsClient.getSecret()).thenReturn(TestSecrets.secret());
    }

    @Test
    void uploadMediaSendsMultipartWithFileAndDescription(@TempDir Path dir) throws IOException {
        givenSecret();

        Path file = Files.writeString(dir.resolve("map.png"), "png-bytes");

        this.server.expect(requestTo("https://mastodon.social/api/v2/media"))
                   .andExpect(method(HttpMethod.POST))
                   .andExpect(header(HttpHeaders.AUTHORIZATION, BEARER))
                   .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                   .andExpect(content().string(containsString("name=\"file\"")))
                   .andExpect(content().string(containsString("png-bytes")))
                   .andExpect(content().string(containsString("name=\"description\"")))
                   .andExpect(content().string(containsString("a map")))
                   .andRespond(withSuccess("""
                       {"id": "m1", "type": "image"}
                       """, MediaType.APPLICATION_JSON));

        Media media = this.client.uploadMedia(file.toFile(), "a map");

        assertThat(media).isEqualTo(new Media("m1"));
        this.server.verify();
    }

    @Test
    void uploadMediaWrapsHttpErrors(@TempDir Path dir) throws IOException {
        givenSecret();

        Path file = Files.writeString(dir.resolve("map.png"), "x");

        this.server.expect(requestTo("https://mastodon.social/api/v2/media")).andRespond(withServerError());

        assertThatThrownBy(() -> this.client.uploadMedia(file.toFile(), "alt"))
            .isInstanceOf(MastodonException.class)
            .hasMessage("Failed to upload media");
    }

    @Test
    void uploadMediaRejectsNulls(@TempDir Path dir) {
        assertThatNullPointerException().isThrownBy(() -> this.client.uploadMedia(null, "alt"));
        assertThatNullPointerException().isThrownBy(() -> this.client.uploadMedia(dir.toFile(), null));
    }

    @Test
    void postStatusWithMediaSendsStatusAndMediaIds() {
        givenSecret();

        this.server.expect(requestTo("https://mastodon.social/api/v1/statuses"))
                   .andExpect(method(HttpMethod.POST))
                   .andExpect(header(HttpHeaders.AUTHORIZATION, BEARER))
                   .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                   .andExpect(jsonPath("$.status").value("hello"))
                   .andExpect(jsonPath("$.media_ids.length()").value(1))
                   .andExpect(jsonPath("$.media_ids[0]").value("m1"))
                   .andRespond(withSuccess("""
                       {"id": "s1", "url": "ignored"}
                       """, MediaType.APPLICATION_JSON));

        Status status = this.client.postStatus("hello", "m1");

        assertThat(status).isEqualTo(new Status("s1"));
        this.server.verify();
    }

    @Test
    void postStatusWithoutMediaOmitsMediaIds() {
        givenSecret();

        this.server.expect(requestTo("https://mastodon.social/api/v1/statuses"))
                   .andExpect(jsonPath("$.status").value("hello"))
                   .andExpect(jsonPath("$.media_ids").doesNotExist())
                   .andRespond(withSuccess("""
                       {"id": "s2"}
                       """, MediaType.APPLICATION_JSON));

        assertThat(this.client.postStatus("hello", null).id()).isEqualTo("s2");
        this.server.verify();
    }

    @Test
    void postStatusWrapsHttpErrors() {
        givenSecret();

        this.server.expect(requestTo("https://mastodon.social/api/v1/statuses")).andRespond(withServerError());

        assertThatThrownBy(() -> this.client.postStatus("hello", null)).isInstanceOf(MastodonException.class);
    }

    @Test
    void postStatusWrapsEmptyBody() {
        givenSecret();

        this.server.expect(requestTo("https://mastodon.social/api/v1/statuses")).andRespond(withSuccess());

        assertThatThrownBy(() -> this.client.postStatus("hello", null)).isInstanceOf(MastodonException.class);
    }
}
