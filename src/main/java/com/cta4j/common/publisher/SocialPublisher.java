package com.cta4j.common.publisher;

import com.cta4j.post.model.Post;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SocialPublisher {
    String getPlatformName();

    void publish(Post post);
}
