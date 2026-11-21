package com.cta4j.bluesky.publisher;

import com.cta4j.aws.client.SecretsClient;
import com.cta4j.aws.dto.Secret;
import com.cta4j.bluesky.client.BlueskyClient;
import com.cta4j.bluesky.dto.Blob;
import com.cta4j.bluesky.dto.ImageBlob;
import com.cta4j.bluesky.dto.Record;
import com.cta4j.bluesky.dto.Session;
import com.cta4j.bluesky.exception.BlueskyException;
import com.cta4j.post.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class BlueskyPublisherTest {
    private static final Session SESSION = new Session("did:plc:abc", "jwt");
    private static final Record RECORD = new Record("cid-1", "at://uri");

    @Mock
    private SecretsClient secretsClient;

    @Mock
    private BlueskyClient blueskyClient;

    private BlueskyPublisher publisher;

    @BeforeEach
    void setUp() {
        this.publisher = new BlueskyPublisher(this.secretsClient, this.blueskyClient);
    }

    private void givenSession() {
        var secret = mock(Secret.class);

        when(secret.bluesky()).thenReturn(new Secret.BlueskySecret("user.bsky.social", "pw"));
        when(this.secretsClient.getSecret()).thenReturn(secret);
        when(this.blueskyClient.createSession("user.bsky.social", "pw")).thenReturn(SESSION);
    }

    @Test
    void reportsPlatformName() {
        assertThat(this.publisher.getPlatformName()).isEqualTo("Bluesky");
    }

    @Test
    void publishesTextOnlyPostWithoutUploadingBlob() {
        givenSession();
        when(this.blueskyClient.createRecord(SESSION, "hello")).thenReturn(RECORD);

        this.publisher.publish(new Post("hello", null));

        verify(this.blueskyClient).createRecord(SESSION, "hello");
        verify(this.blueskyClient, never()).uploadBlob(any(), any());
    }

    @Test
    void uploadsBlobThenCreatesRecordWithImage() {
        givenSession();

        var file = new File("map.png");
        var blob = new Blob("blob", new Blob.Ref("link"), "image/png", 10L);
        var image = new ImageBlob(blob, "alt text");

        when(this.blueskyClient.uploadBlob(SESSION, file)).thenReturn(blob);
        when(this.blueskyClient.createRecord(SESSION, "hello", image)).thenReturn(RECORD);

        this.publisher.publish(new Post("hello", new Post.Image(file, "alt text")));

        InOrder order = inOrder(this.blueskyClient);

        order.verify(this.blueskyClient).createSession("user.bsky.social", "pw");
        order.verify(this.blueskyClient).uploadBlob(SESSION, file);
        order.verify(this.blueskyClient).createRecord(SESSION, "hello", image);
    }

    @Test
    void propagatesClientFailures() {
        givenSession();
        when(this.blueskyClient.createRecord(SESSION, "hello")).thenThrow(new BlueskyException("fail", null));

        var post = new Post("hello", null);

        assertThatThrownBy(() -> this.publisher.publish(post)).isInstanceOf(BlueskyException.class);
    }
}
