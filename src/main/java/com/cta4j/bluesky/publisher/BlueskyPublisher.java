package com.cta4j.bluesky.publisher;

import com.cta4j.aws.client.SecretsClient;
import com.cta4j.aws.dto.Secret;
import com.cta4j.bluesky.client.BlueskyClient;
import com.cta4j.bluesky.dto.Blob;
import com.cta4j.bluesky.dto.ImageBlob;
import com.cta4j.bluesky.dto.Record;
import com.cta4j.bluesky.dto.Session;
import com.cta4j.bluesky.exception.BlueskyException;
import com.cta4j.post.model.Post;
import com.cta4j.common.publisher.SocialPublisher;
import com.rollbar.notifier.Rollbar;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NullMarked
public final class BlueskyPublisher implements SocialPublisher {
    private static final String PLATFORM_NAME = "Bluesky";

    private final SecretsClient secretsClient;
    private final BlueskyClient blueskyClient;
    private final Rollbar rollbar;

    @Autowired
    public BlueskyPublisher(
        SecretsClient secretsClient,
        BlueskyClient blueskyClient,
        Rollbar rollbar
    ) {
        this.secretsClient = secretsClient;
        this.blueskyClient = blueskyClient;
        this.rollbar = rollbar;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {
        Secret.BlueskySecret secret = this.secretsClient.getSecret().bluesky();

        Session session = this.blueskyClient.createSession(secret.identifier(), secret.appPassword());

        if (post.image() == null) {
            Record record = this.blueskyClient.createRecord(session, post.text());

            log.info("Post without image created on Bluesky with ID {}", record.cid());

            return;
        }

        ImageBlob image = null;

        try {
            Blob blob = this.blueskyClient.uploadBlob(session, post.image().file());

            image = new ImageBlob(blob, post.image().altText());
        } catch (BlueskyException e) {
            var message = "Failed to upload post blob to Bluesky";

            log.error(message, e);
            this.rollbar.error(e, message);
        }

        Record record = this.blueskyClient.createRecord(session, post.text(), image);

        log.info("Post created on Bluesky with ID {}", record.cid());
    }
}
