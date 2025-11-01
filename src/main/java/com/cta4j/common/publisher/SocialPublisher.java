package com.cta4j.common.publisher;

import com.cta4j.common.dto.Post;

public interface SocialPublisher {
    String getPlatformName();

    void publish(Post payload);
}
