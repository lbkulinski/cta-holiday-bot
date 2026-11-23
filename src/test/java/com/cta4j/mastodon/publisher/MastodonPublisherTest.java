package com.cta4j.mastodon.publisher;

import com.cta4j.mastodon.client.MastodonClient;
import com.cta4j.mastodon.dto.Media;
import com.cta4j.mastodon.dto.Status;
import com.cta4j.mastodon.exception.MastodonException;
import com.cta4j.post.model.Post;
import com.rollbar.notifier.Rollbar;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MastodonPublisherTest {
    @Mock
    private MastodonClient mastodonClient;

    @Mock
    private Rollbar rollbar;

    private MastodonPublisher publisher;

    @BeforeEach
    void setUp() {
        this.publisher = new MastodonPublisher(this.mastodonClient, this.rollbar);
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
        verifyNoInteractions(this.rollbar);
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
        verifyNoInteractions(this.rollbar);
    }

    @Test
    void fallsBackToTextOnlyStatusAndReportsWhenMediaUploadFails() {
        var file = new File("map.png");
        var failure = new MastodonException("upload failed", null);

        when(this.mastodonClient.uploadMedia(file, "alt")).thenThrow(failure);
        when(this.mastodonClient.postStatus("hello", null)).thenReturn(new Status("s1"));

        this.publisher.publish(new Post("hello", new Post.Image(file, "alt")));

        verify(this.mastodonClient).postStatus("hello", null);
        verify(this.rollbar).error(failure, "Failed to upload post media to Mastodon");
    }

    @Test
    void propagatesFailureOfTheFallbackStatus() {
        var file = new File("map.png");

        when(this.mastodonClient.uploadMedia(file, "alt")).thenThrow(new MastodonException("upload failed", null));
        when(this.mastodonClient.postStatus("hello", null)).thenThrow(new MastodonException("post failed", null));

        var post = new Post("hello", new Post.Image(file, "alt"));

        assertThatThrownBy(() -> this.publisher.publish(post))
            .isInstanceOf(MastodonException.class)
            .hasMessage("post failed");
    }

    @Test
    void doesNotFallBackOnNonMastodonExceptions() {
        var file = new File("map.png");

        when(this.mastodonClient.uploadMedia(file, "alt")).thenThrow(new IllegalStateException("boom"));

        var post = new Post("hello", new Post.Image(file, "alt"));

        assertThatThrownBy(() -> this.publisher.publish(post)).isInstanceOf(IllegalStateException.class);

        verify(this.mastodonClient, never()).postStatus(anyString(), any());
        verifyNoInteractions(this.rollbar);
    }

    @Test
    void rejectsNullPost() {
        assertThatNullPointerException().isThrownBy(() -> this.publisher.publish(null));
    }
}
