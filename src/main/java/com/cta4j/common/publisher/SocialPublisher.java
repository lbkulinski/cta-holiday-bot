package com.cta4j.common.publisher;

import com.cta4j.common.dto.PostPayload;

public interface SocialPublisher {
    void publish(PostPayload payload);
}
