package app.cta4j.twitter.publisher;

import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.SocialPublisher;
import app.cta4j.twitter.dto.Tweet;
import app.cta4j.twitter.service.TwitterMediaService;
import app.cta4j.twitter.service.TweetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class TwitterPublisher implements SocialPublisher {
    private static final Logger log = LoggerFactory.getLogger(TwitterPublisher.class);

    private static final String PLATFORM_NAME = "Twitter";

    private final TwitterMediaService mediaService;
    private final TweetService tweetService;

    @Autowired
    public TwitterPublisher(TwitterMediaService mediaService, TweetService tweetService) {
        this.mediaService = mediaService;
        this.tweetService = tweetService;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {
        Objects.requireNonNull(post);

        if (post.media() == null) {
            Tweet tweet = this.tweetService.postTweet(post.text());

            log.info("Tweet created without image on Twitter with ID {}", tweet.id());

            return;
        }

        String mediaId = this.mediaService.uploadMedia(post.media())
                                          .id();

        Tweet tweet = this.tweetService.postTweet(post.text(), mediaId);

        log.info("Tweet created on Twitter with ID {}", tweet.id());
    }
}
