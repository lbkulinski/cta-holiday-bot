package com.cta4j.mastodon.publisher;

import com.cta4j.common.publisher.SocialPublisher;
import com.cta4j.mastodon.client.MastodonClient;
import com.cta4j.mastodon.dto.Status;
import com.cta4j.mastodon.exception.MastodonException;
import com.cta4j.post.model.Post;
import com.rollbar.notifier.Rollbar;
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
    private final Rollbar rollbar;

    @Autowired
    public MastodonPublisher(
        MastodonClient mastodonClient,
        Rollbar rollbar
    ) {
        this.mastodonClient = mastodonClient;
        this.rollbar = rollbar;
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

        String mediaId = null;

        try {
            mediaId = this.mastodonClient.uploadMedia(post.image().file(), post.image().altText()).id();
        } catch (MastodonException e) {
            var message = "Failed to upload post media to Mastodon";

            log.error(message, e);
            this.rollbar.error(e, message);
        }

        Status status = this.mastodonClient.postStatus(post.text(), mediaId);

        log.info("Status created on Mastodon with ID {}", status.id());
    }
}
