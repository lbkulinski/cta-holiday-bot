package com.cta4j.bluesky.client;

import com.cta4j.bluesky.dto.Blob;
import com.cta4j.bluesky.dto.ImageBlob;
import com.cta4j.bluesky.dto.Record;
import com.cta4j.bluesky.dto.Session;
import com.cta4j.bluesky.exception.BlueskyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BlueskyClientTest {
    private static final String BASE = "https://bsky.social/xrpc/";

    private static final Session SESSION = new Session("did:plc:abc", "the-jwt");

    private MockRestServiceServer server;
    private BlueskyClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        this.server = MockRestServiceServer.bindTo(builder).build();
        this.client = new BlueskyClient(builder);
    }

    @Test
    void createSessionPostsCredentialsAndParsesResponse() {
        this.server.expect(requestTo(BASE + "com.atproto.server.createSession"))
                   .andExpect(method(HttpMethod.POST))
                   .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                   .andExpect(jsonPath("$.identifier").value("me.bsky.social"))
                   .andExpect(jsonPath("$.password").value("secret"))
                   .andRespond(withSuccess("""
                       {"did": "did:plc:abc", "accessJwt": "jwt", "refreshJwt": "ignored"}
                       """, MediaType.APPLICATION_JSON));

        Session session = this.client.createSession("me.bsky.social", "secret");

        assertThat(session).isEqualTo(new Session("did:plc:abc", "jwt"));
        this.server.verify();
    }

    @Test
    void createSessionWrapsHttpErrors() {
        this.server.expect(requestTo(BASE + "com.atproto.server.createSession"))
                   .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> this.client.createSession("me", "bad"))
            .isInstanceOf(BlueskyException.class)
            .hasMessage("Failed to create session");
    }

    @Test
    void createSessionWrapsEmptyBody() {
        this.server.expect(requestTo(BASE + "com.atproto.server.createSession"))
                   .andRespond(withSuccess());

        assertThatThrownBy(() -> this.client.createSession("me", "pw")).isInstanceOf(BlueskyException.class);
    }

    @Test
    void createSessionRejectsNulls() {
        assertThatNullPointerException().isThrownBy(() -> this.client.createSession(null, "pw"));
        assertThatNullPointerException().isThrownBy(() -> this.client.createSession("me", null));
    }

    @Test
    void uploadBlobSendsFileBytesWithAuthAndParsesBlob(@TempDir Path dir) throws IOException {
        byte[] bytes = {10, 20, 30, 40};
        Path file = Files.write(dir.resolve("map.png"), bytes);

        this.server.expect(requestTo(BASE + "com.atproto.repo.uploadBlob"))
                   .andExpect(method(HttpMethod.POST))
                   .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer the-jwt"))
                   .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
                   .andExpect(content().bytes(bytes))
                   .andRespond(withSuccess("""
                       {"blob": {"$type": "blob", "ref": {"$link": "bafy"}, "mimeType": "image/png", "size": 4}}
                       """, MediaType.APPLICATION_JSON));

        Blob blob = this.client.uploadBlob(SESSION, file.toFile());

        assertThat(blob).isEqualTo(new Blob("blob", new Blob.Ref("bafy"), "image/png", 4));
        this.server.verify();
    }

    @Test
    void uploadBlobWrapsMissingFileWithoutCallingServer(@TempDir Path dir) {
        var missing = dir.resolve("missing.png").toFile();

        assertThatThrownBy(() -> this.client.uploadBlob(SESSION, missing))
            .isInstanceOf(BlueskyException.class)
            .hasMessage("Failed to read file while uploading blob")
            .hasCauseInstanceOf(IOException.class);

        this.server.verify();
    }

    @Test
    void uploadBlobWrapsHttpErrors(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("map.png"), new byte[] {1});

        this.server.expect(requestTo(BASE + "com.atproto.repo.uploadBlob")).andRespond(withServerError());

        assertThatThrownBy(() -> this.client.uploadBlob(SESSION, file.toFile()))
            .isInstanceOf(BlueskyException.class)
            .hasMessage("Failed to upload blob");
    }

    @Test
    void createRecordWithoutImageOmitsEmbed() {
        this.server.expect(requestTo(BASE + "com.atproto.repo.createRecord"))
                   .andExpect(method(HttpMethod.POST))
                   .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer the-jwt"))
                   .andExpect(jsonPath("$.repo").value("did:plc:abc"))
                   .andExpect(jsonPath("$.collection").value("app.bsky.feed.post"))
                   .andExpect(jsonPath("$.record['$type']").value("app.bsky.feed.post"))
                   .andExpect(jsonPath("$.record.text").value("hello"))
                   .andExpect(jsonPath("$.record.createdAt").isNotEmpty())
                   .andExpect(jsonPath("$.record.embed").doesNotExist())
                   .andRespond(withSuccess("""
                       {"cid": "cid-1", "uri": "at://did:plc:abc/app.bsky.feed.post/1"}
                       """, MediaType.APPLICATION_JSON));

        Record record = this.client.createRecord(SESSION, "hello");

        assertThat(record).isEqualTo(new Record("cid-1", "at://did:plc:abc/app.bsky.feed.post/1"));
        this.server.verify();
    }

    @Test
    void createRecordWithImageIncludesEmbed() {
        var blob = new Blob("blob", new Blob.Ref("bafy"), "image/png", 4);
        var image = new ImageBlob(blob, "alt text");

        this.server.expect(requestTo(BASE + "com.atproto.repo.createRecord"))
                   .andExpect(jsonPath("$.record.embed['$type']").value("app.bsky.embed.images"))
                   .andExpect(jsonPath("$.record.embed.images.length()").value(1))
                   .andExpect(jsonPath("$.record.embed.images[0].alt").value("alt text"))
                   .andExpect(jsonPath("$.record.embed.images[0].image.ref['$link']").value("bafy"))
                   .andExpect(jsonPath("$.record.embed.images[0].image.mimeType").value("image/png"))
                   .andRespond(withSuccess("""
                       {"cid": "cid-2", "uri": "at://uri"}
                       """, MediaType.APPLICATION_JSON));

        Record record = this.client.createRecord(SESSION, "hello", image);

        assertThat(record.cid()).isEqualTo("cid-2");
        this.server.verify();
    }

    @Test
    void createRecordWrapsHttpErrors() {
        this.server.expect(requestTo(BASE + "com.atproto.repo.createRecord")).andRespond(withServerError());

        assertThatThrownBy(() -> this.client.createRecord(SESSION, "hello"))
            .isInstanceOf(BlueskyException.class)
            .hasMessage("Failed to create record");
    }

    @Test
    void createRecordRejectsNullSession() {
        assertThatNullPointerException().isThrownBy(() -> this.client.createRecord(null, "hello"));
    }

    @Test
    void createRecordRejectsNullText() {
        assertThatNullPointerException().isThrownBy(() -> this.client.createRecord(SESSION, null));
    }

    @Test
    void createSessionDoesNotSendAuthorizationHeader() {
        this.server.expect(requestTo(BASE + "com.atproto.server.createSession"))
                   .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                   .andRespond(withSuccess("""
                       {"did": "d", "accessJwt": "j"}
                       """, MediaType.APPLICATION_JSON));

        this.client.createSession("me", "pw");

        this.server.verify();
    }
}
