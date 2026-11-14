package app.cta4j.bluesky.publisher;

import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.SocialPublisher;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NullMarked
public final class BlueskyPublisher implements SocialPublisher {
    private static final String PLATFORM_NAME = "Bluesky";

//    private final BlueskySessionService sessionService;
//    private final BlueskyBlobService blobService;
//    private final BlueskyRecordService recordService;
//
//    @Autowired
//    public BlueskyPublisher(
//        BlueskySessionService sessionService,
//        BlueskyBlobService blobService,
//        BlueskyRecordService recordService
//    ) {
//        this.sessionService = sessionService;
//        this.blobService = blobService;
//        this.recordService = recordService;
//    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }

    @Override
    public void publish(Post post) {
//        Session session = this.sessionService.createSession();
//
//        if (post.media() == null) {
//            BlueskyRecord record = this.recordService.createRecord(session, post.text());
//
//            log.info("Post without image created on Bluesky with ID {}", record.cid());
//
//            return;
//        }
//
//        File media = post.media();
//
//        BlueskyBlob blob = this.blobService.uploadBlob(session, media);
//
//        BlueskyRecord record = this.recordService.createRecord(session, post.text(), blob);
//
//        log.info("Post created on Bluesky with ID {}", record.cid());
    }
}
