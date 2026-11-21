package com.cta4j.mastodon.publisher;

import com.cta4j.common.publisher.SocialPublisher;
import com.cta4j.mastodon.client.MastodonClient;
import com.cta4j.mastodon.dto.Status;
import com.cta4j.post.model.Post;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
@NullMarked
public final class MastodonPublisher implements SocialPublisher {
    private static final String PLATFORM_NAME = "Mastodon";

    private final MastodonClient mastodonClient;

    @Autowired
    public MastodonPublisher(MastodonClient mastodonClient) {
        this.mastodonClient = mastodonClient;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {
        Objects.requireNonNull(post);

        if (post.image() == null) {
            Status status = this.mastodonClient.postStatus(post.text(), null);

            log.info("Status created without image on Mastodon with ID {}", status.id());

            return;
        }

        String mediaId = this.mastodonClient.uploadMedia(post.image().file(), post.image().altText()).id();

        Status status = this.mastodonClient.postStatus(post.text(), mediaId);

        log.info("Status created on Mastodon with ID {}", status.id());
    }
}
