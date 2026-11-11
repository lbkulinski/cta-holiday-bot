package app.cta4j.twitter.publisher;

import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.SocialPublisher;
import app.cta4j.twitter.dto.Tweet;
import app.cta4j.twitter.client.TwitterClient;
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

    @Autowired
    public TwitterPublisher(TwitterClient twitterClient) {
        this.twitterClient = twitterClient;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {
        Objects.requireNonNull(post);

        if (post.media() == null) {
            Tweet tweet = this.twitterClient.postTweet(post.text());

            log.info("Tweet created without image on Twitter with ID {}", tweet.id());

            return;
        }

        String mediaId = this.twitterClient.uploadMedia(post.media()).id();

        Tweet tweet = this.twitterClient.postTweet(post.text(), mediaId);

        log.info("Tweet created on Twitter with ID {}", tweet.id());
    }
}
