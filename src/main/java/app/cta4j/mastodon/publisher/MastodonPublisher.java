package app.cta4j.mastodon.publisher;

import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.SocialPublisher;
import app.cta4j.mastodon.service.MastodonMediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class MastodonPublisher implements SocialPublisher {
    private static final String PLATFORM_NAME = "Mastodon";

    private final MastodonMediaService mediaService;

    @Autowired
    public MastodonPublisher(
        MastodonMediaService mediaService
    ) {
        this.mediaService = mediaService;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {

    }
}
