package app.cta4j.bluesky.publisher;

import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.SocialPublisher;
import org.springframework.stereotype.Component;

@Component
public final class BlueskyPublisher implements SocialPublisher {
    private static final String PLATFORM_NAME = "Bluesky";

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {

    }
}
