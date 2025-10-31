package com.cta4j.common.publisher;

import com.cta4j.common.dto.PostPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class MultiplatformPublisher {
    private final List<SocialPublisher> socialPublishers;

    @Autowired
    public MultiplatformPublisher(List<SocialPublisher> socialPublishers) {
        this.socialPublishers = socialPublishers;
    }

    public void publish(PostPayload postPayload) {
        for (SocialPublisher publisher : this.socialPublishers) {
            publisher.publish(postPayload);
        }
    }
}
