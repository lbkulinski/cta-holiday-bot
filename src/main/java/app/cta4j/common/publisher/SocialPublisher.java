package app.cta4j.common.publisher;

import app.cta4j.common.dto.Post;

public interface SocialPublisher {
    String getPlatformName();

    void publish(Post post);
}
