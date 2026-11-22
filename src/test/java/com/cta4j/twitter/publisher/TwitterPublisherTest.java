package com.cta4j.twitter.publisher;

import com.cta4j.post.model.Post;
import com.cta4j.twitter.client.TwitterClient;
import com.cta4j.twitter.dto.Media;
import com.cta4j.twitter.dto.Tweet;
import com.cta4j.twitter.exception.TwitterException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterPublisherTest {
    @Mock
    private TwitterClient twitterClient;

    @Mock
    private Rollbar rollbar;

    private TwitterPublisher publisher;

    @BeforeEach
    void setUp() {
        this.publisher = new TwitterPublisher(this.twitterClient, this.rollbar);
    }

    @Test
    void reportsPlatformName() {
        assertThat(this.publisher.getPlatformName()).isEqualTo("Twitter");
    }

    @Test
    void publishesTextOnlyPostWithoutUploadingMedia() {
        when(this.twitterClient.postTweet("hello")).thenReturn(new Tweet("t1", "hello"));

        this.publisher.publish(new Post("hello", null));

        verify(this.twitterClient).postTweet("hello");
        verify(this.twitterClient, never()).uploadMedia(any());
        verify(this.twitterClient, never()).setMediaAltText(anyString(), anyString());
        verifyNoInteractions(this.rollbar);
    }

    @Test
    void uploadsMediaSetsAltTextThenPostsTweet() {
        var file = new File("map.png");

        when(this.twitterClient.uploadMedia(file)).thenReturn(new Media("m1"));
        when(this.twitterClient.postTweet("hello", "m1")).thenReturn(new Tweet("t1", "hello"));

        this.publisher.publish(new Post("hello", new Post.Image(file, "alt text")));

        InOrder order = inOrder(this.twitterClient);

        order.verify(this.twitterClient).uploadMedia(file);
        order.verify(this.twitterClient).setMediaAltText("m1", "alt text");
        order.verify(this.twitterClient).postTweet("hello", "m1");
        verifyNoInteractions(this.rollbar);
    }

    @Test
    void fallsBackToTextOnlyTweetAndReportsWhenMediaUploadFails() {
        var file = new File("map.png");
        var failure = new TwitterException("upload failed");

        when(this.twitterClient.uploadMedia(file)).thenThrow(failure);
        when(this.twitterClient.postTweet("hello", null)).thenReturn(new Tweet("t1", "hello"));

        this.publisher.publish(new Post("hello", new Post.Image(file, "alt")));

        verify(this.twitterClient, never()).setMediaAltText(anyString(), anyString());
        verify(this.twitterClient).postTweet("hello", null);
        verify(this.rollbar).error(failure, "Failed to upload post media to Twitter");
    }

    @Test
    void stillPostsTweetWithMediaAndReportsWhenAltTextFails() {
        var file = new File("map.png");
        var failure = new TwitterException("alt text failed");

        when(this.twitterClient.uploadMedia(file)).thenReturn(new Media("m1"));
        doThrow(failure).when(this.twitterClient).setMediaAltText("m1", "alt");
        when(this.twitterClient.postTweet("hello", "m1")).thenReturn(new Tweet("t1", "hello"));

        this.publisher.publish(new Post("hello", new Post.Image(file, "alt")));

        verify(this.twitterClient).postTweet("hello", "m1");
        verify(this.rollbar).error(failure, "Failed to set media alt text on Twitter");
        verify(this.rollbar, never()).error(any(Throwable.class), eq("Failed to upload post media to Twitter"));
    }

    @Test
    void propagatesFailureOfTheFallbackTweet() {
        var file = new File("map.png");

        when(this.twitterClient.uploadMedia(file)).thenThrow(new TwitterException("upload failed"));
        when(this.twitterClient.postTweet("hello", null)).thenThrow(new TwitterException("post failed"));

        var post = new Post("hello", new Post.Image(file, "alt"));

        assertThatThrownBy(() -> this.publisher.publish(post))
            .isInstanceOf(TwitterException.class)
            .hasMessage("post failed");
    }

    @Test
    void propagatesFailureOfTheTweetWithMedia() {
        var file = new File("map.png");

        when(this.twitterClient.uploadMedia(file)).thenReturn(new Media("m1"));
        when(this.twitterClient.postTweet("hello", "m1")).thenThrow(new TwitterException("post failed"));

        var post = new Post("hello", new Post.Image(file, "alt"));

        assertThatThrownBy(() -> this.publisher.publish(post)).isInstanceOf(TwitterException.class);

        verifyNoInteractions(this.rollbar);
    }

    @Test
    void doesNotFallBackOnNonTwitterExceptions() {
        var file = new File("map.png");

        when(this.twitterClient.uploadMedia(file)).thenThrow(new IllegalStateException("boom"));

        var post = new Post("hello", new Post.Image(file, "alt"));

        assertThatThrownBy(() -> this.publisher.publish(post)).isInstanceOf(IllegalStateException.class);

        verify(this.twitterClient, never()).postTweet(anyString(), any());
        verifyNoInteractions(this.rollbar);
    }

    @Test
    void rejectsNullPost() {
        assertThatNullPointerException().isThrownBy(() -> this.publisher.publish(null));
    }
}
