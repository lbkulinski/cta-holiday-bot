package app.cta4j.bluesky.publisher;

import app.cta4j.aws.client.SecretsClient;
import app.cta4j.aws.dto.Secret;
import app.cta4j.bluesky.client.BlueskyClient;
import app.cta4j.bluesky.dto.Blob;
import app.cta4j.bluesky.dto.Image;
import app.cta4j.bluesky.dto.Record;
import app.cta4j.bluesky.dto.Session;
import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.SocialPublisher;
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

    @Autowired
    public BlueskyPublisher(
        SecretsClient secretsClient,
        BlueskyClient blueskyClient
    ) {
        this.secretsClient = secretsClient;
        this.blueskyClient = blueskyClient;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {
        Secret.BlueskySecret secret = this.secretsClient.getSecret().bluesky();

        Session session = this.blueskyClient.createSession(secret.identifier(), secret.appPassword());

        if (post.media() == null) {
            Record record = this.blueskyClient.createRecord(session, post.text());

            log.info("Post without image created on Bluesky with ID {}", record.cid());

            return;
        }

        Blob blob = this.blueskyClient.uploadBlob(session, post.media());

        var image = new Image(blob, "");

        Record record = this.blueskyClient.createRecord(session, post.text(), image);

        log.info("Post created on Bluesky with ID {}", record.cid());
    }
}
