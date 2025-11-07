package app.cta4j.bluesky.publisher;

import app.cta4j.bluesky.dto.Blob;
import app.cta4j.bluesky.dto.Session;
import app.cta4j.bluesky.service.BlueskyBlobService;
import app.cta4j.bluesky.service.BlueskySessionService;
import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.SocialPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public final class BlueskyPublisher implements SocialPublisher {
    private static final String PLATFORM_NAME = "Bluesky";

    private final BlueskySessionService sessionService;
    private final BlueskyBlobService blobService;

    @Autowired
    public BlueskyPublisher(
        BlueskySessionService sessionService,
        BlueskyBlobService blobService
    ) {
        this.sessionService = sessionService;
        this.blobService = blobService;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {
        Session session = this.sessionService.createSession();

        File media = post.media();

        Blob blob = this.blobService.uploadBlob(session, media);
    }
}
