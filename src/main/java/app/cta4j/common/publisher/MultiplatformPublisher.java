package app.cta4j.common.publisher;

import app.cta4j.common.dto.Post;
import com.rollbar.notifier.Rollbar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public final class MultiplatformPublisher {
    private static final Logger log = LoggerFactory.getLogger(MultiplatformPublisher.class);

    private final List<SocialPublisher> socialPublishers;
    private final Rollbar rollbar;

    @Autowired
    public MultiplatformPublisher(
        List<SocialPublisher> socialPublishers,
        Rollbar rollbar
    ) {
        this.socialPublishers = socialPublishers;
        this.rollbar = rollbar;
    }

    public void publish(Post post) {
        for (SocialPublisher publisher : this.socialPublishers) {
            try {
                publisher.publish(post);
            } catch (Exception e) {
                String platformName = publisher.getPlatformName();

                String message = String.format("Failed to publish post on %s", platformName);

                log.error(message, e);

                this.rollbar.error(e, message);
            }
        }
    }
}
