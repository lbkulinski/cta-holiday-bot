package app.cta4j.twitter.publisher;

import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.SocialPublisher;
import app.cta4j.twitter.service.MediaService;
import app.cta4j.twitter.service.TweetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Objects;

@Component
public final class TwitterPublisher implements SocialPublisher {
    private static final String PLATFORM_NAME = "Twitter";

    private final MediaService mediaService;
    private final TweetService tweetService;

    @Autowired
    public TwitterPublisher(MediaService mediaService, TweetService tweetService) {
        this.mediaService = mediaService;
        this.tweetService = tweetService;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post payload) {
        Objects.requireNonNull(payload);

        String text = payload.text();

        File media = payload.media();

        if (media == null) {
            this.tweetService.postTweet(text);

            return;
        }

        String mediaId = this.mediaService.uploadMedia(media)
                                          .id();

        this.tweetService.postTweet(text, mediaId);
    }
}
