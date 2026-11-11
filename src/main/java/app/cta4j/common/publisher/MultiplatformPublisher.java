package app.cta4j.common.publisher;

import app.cta4j.common.dto.Post;
import com.rollbar.notifier.Rollbar;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@NullMarked
public final class MultiplatformPublisher {
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
        Objects.requireNonNull(post);

        for (SocialPublisher publisher : this.socialPublishers) {
            try {
                publisher.publish(post);
            } catch (Exception e) {
                String platformName = publisher.getPlatformName();

                var message = "Failed to publish post on %s".formatted(platformName);

                log.error(message, e);
                this.rollbar.error(e, message);
            }
        }
    }
}
