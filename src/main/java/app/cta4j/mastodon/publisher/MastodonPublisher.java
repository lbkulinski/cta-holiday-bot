package app.cta4j.mastodon.publisher;

import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.SocialPublisher;
import app.cta4j.mastodon.dto.MastodonStatus;
import app.cta4j.mastodon.service.MastodonMediaService;
import app.cta4j.mastodon.service.MastodonStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class MastodonPublisher implements SocialPublisher {
    private static final Logger log = LoggerFactory.getLogger(MastodonPublisher.class);

    private static final String PLATFORM_NAME = "Mastodon";

    private final MastodonMediaService mediaService;
    private final MastodonStatusService statusService;

    @Autowired
    public MastodonPublisher(
        MastodonMediaService mediaService,
        MastodonStatusService statusService
    ) {
        this.mediaService = mediaService;
        this.statusService = statusService;
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {
        Objects.requireNonNull(post);

        if (post.media() == null) {
            MastodonStatus status = this.statusService.postStatus(post.text());

            log.info("Status created without image on Mastodon with ID {}", status.id());

            return;
        }

        String mediaId = this.mediaService.uploadMedia(post.media())
                                          .id();

        MastodonStatus status = this.statusService.postStatus(post.text(), mediaId);

        log.info("Status created on Mastodon with ID {}", status.id());
    }
}
