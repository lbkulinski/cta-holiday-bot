package com.cta4j.common.publisher;

import com.cta4j.common.dto.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class MultiplatformPublisher {
    private static final Logger log = LoggerFactory.getLogger(MultiplatformPublisher.class);

    private final List<SocialPublisher> socialPublishers;

    @Autowired
    public MultiplatformPublisher(List<SocialPublisher> socialPublishers) {
        this.socialPublishers = socialPublishers;
    }

    public void publish(Post post) {
        for (SocialPublisher publisher : this.socialPublishers) {
            try {
                publisher.publish(post);
            } catch (Exception e) {
                String platformName = publisher.getPlatformName();

                String message = String.format("Failed to publish post on %s", platformName);

                log.error(message, e);
            }
        }
    }
}
