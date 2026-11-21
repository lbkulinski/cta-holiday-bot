package com.cta4j.twitter.publisher;

import com.cta4j.post.model.Post;
import com.cta4j.twitter.client.TwitterClient;
import com.cta4j.twitter.dto.Media;
import com.cta4j.twitter.dto.Tweet;
import com.cta4j.twitter.exception.TwitterException;
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
class TwitterPublisherTest {
    @Mock
    private TwitterClient twitterClient;

    private TwitterPublisher publisher;

    @BeforeEach
    void setUp() {
        this.publisher = new TwitterPublisher(this.twitterClient);
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
    }

    @Test
    void uploadsMediaSetsAltTextThenPostsTweet() {
        var file = new File("map.png");

        when(this.twitterClient.uploadMedia(file)).thenReturn(new Media("m1", "key"));
        when(this.twitterClient.postTweet("hello", "m1")).thenReturn(new Tweet("t1", "hello"));

        this.publisher.publish(new Post("hello", new Post.Image(file, "alt text")));

        InOrder order = inOrder(this.twitterClient);

        order.verify(this.twitterClient).uploadMedia(file);
        order.verify(this.twitterClient).setMediaAltText("m1", "alt text");
        order.verify(this.twitterClient).postTweet("hello", "m1");
    }

    @Test
    void doesNotPostTweetWhenAltTextFails() {
        var file = new File("map.png");

        when(this.twitterClient.uploadMedia(file)).thenReturn(new Media("m1", "key"));
        org.mockito.Mockito.doThrow(new TwitterException("fail", null))
                           .when(this.twitterClient).setMediaAltText("m1", "alt");

        var post = new Post("hello", new Post.Image(file, "alt"));

        assertThatThrownBy(() -> this.publisher.publish(post)).isInstanceOf(TwitterException.class);

        verify(this.twitterClient, never()).postTweet(anyString(), any());
    }

    @Test
    void rejectsNullPost() {
        assertThatNullPointerException().isThrownBy(() -> this.publisher.publish(null));
    }
}
