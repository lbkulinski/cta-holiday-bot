package com.cta4j.twitter.publisher;

import com.cta4j.post.model.Post;
import com.cta4j.common.publisher.SocialPublisher;
import com.cta4j.twitter.dto.Tweet;
import com.cta4j.twitter.client.TwitterClient;
import com.cta4j.twitter.exception.TwitterException;
import com.rollbar.notifier.Rollbar;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
@NullMarked
public final class TwitterPublisher implements SocialPublisher {
    private static final String PLATFORM_NAME = "Twitter";

    private final TwitterClient twitterClient;
    private final Rollbar rollbar;

    @Autowired
    public TwitterPublisher(
        TwitterClient twitterClient,
        Rollbar rollbar
    ) {
        this.twitterClient = twitterClient;
        this.rollbar = rollbar;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {
        Objects.requireNonNull(post);

        if (post.image() == null) {
            Tweet tweet = this.twitterClient.postTweet(post.text());

            log.info("Tweet created without image with ID {}", tweet.id());

            return;
        }

        String mediaId = null;

        try {
            mediaId = this.twitterClient.uploadMedia(post.image().file()).id();
        } catch (TwitterException e) {
            var message = "Failed to upload post media to Twitter";

            log.error(message, e);
            this.rollbar.error(e, message);
        }

        if (mediaId != null) {
            try {
                this.twitterClient.setMediaAltText(mediaId, post.image().altText());
            } catch (TwitterException e) {
                var message = "Failed to set media alt text on Twitter";

                log.error(message, e);
                this.rollbar.error(e, message);
            }
        }

        Tweet tweet = this.twitterClient.postTweet(post.text(), mediaId);

        log.info("Tweet created with ID {}", tweet.id());
    }
}
