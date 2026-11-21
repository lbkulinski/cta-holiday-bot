package com.cta4j.mastodon.publisher;

import com.cta4j.mastodon.client.MastodonClient;
import com.cta4j.mastodon.dto.Media;
import com.cta4j.mastodon.dto.Status;
import com.cta4j.mastodon.exception.MastodonException;
import com.cta4j.post.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MastodonPublisherTest {
    @Mock
    private MastodonClient mastodonClient;

    private MastodonPublisher publisher;

    @BeforeEach
    void setUp() {
        this.publisher = new MastodonPublisher(this.mastodonClient);
    }

    @Test
    void reportsPlatformName() {
        assertThat(this.publisher.getPlatformName()).isEqualTo("Mastodon");
    }

    @Test
    void publishesTextOnlyPostWithoutUploadingMedia() {
        when(this.mastodonClient.postStatus("hello", null)).thenReturn(new Status("s1"));

        this.publisher.publish(new Post("hello", null));

        verify(this.mastodonClient).postStatus("hello", null);
        verify(this.mastodonClient, never()).uploadMedia(any(), anyString());
    }

    @Test
    void uploadsMediaWithAltTextThenPostsStatus() {
        var file = new File("map.png");

        when(this.mastodonClient.uploadMedia(file, "alt text")).thenReturn(new Media("m1"));
        when(this.mastodonClient.postStatus("hello", "m1")).thenReturn(new Status("s1"));

        this.publisher.publish(new Post("hello", new Post.Image(file, "alt text")));

        InOrder order = inOrder(this.mastodonClient);

        order.verify(this.mastodonClient).uploadMedia(file, "alt text");
        order.verify(this.mastodonClient).postStatus("hello", "m1");
    }

    @Test
    void doesNotPostStatusWhenMediaUploadFails() {
        var file = new File("map.png");

        when(this.mastodonClient.uploadMedia(file, "alt")).thenThrow(new MastodonException("fail", null));

        var post = new Post("hello", new Post.Image(file, "alt"));

        assertThatThrownBy(() -> this.publisher.publish(post)).isInstanceOf(MastodonException.class);

        verify(this.mastodonClient, never()).postStatus(anyString(), any());
    }

    @Test
    void rejectsNullPost() {
        assertThatNullPointerException().isThrownBy(() -> this.publisher.publish(null));
    }
}
