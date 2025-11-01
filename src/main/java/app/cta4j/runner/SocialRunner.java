package app.cta4j.runner;

import app.cta4j.common.service.PostService;
import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.MultiplatformPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SocialRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SocialRunner.class);

    private final PostService postService;
    private final MultiplatformPublisher multiplatformPublisher;

    private final String trainRun;

    @Autowired
    public SocialRunner(
        PostService postService,
        MultiplatformPublisher multiplatformPublisher,
        @Value("${app.cta.train.run}") String trainRun
    ) {
        this.postService = postService;
        this.multiplatformPublisher = multiplatformPublisher;
        this.trainRun = trainRun;
    }

    @Override
    public void run(String... args) {
        Optional<Post> optionalPost = this.postService.buildPost(this.trainRun);

        if (optionalPost.isEmpty()) {
            log.info("No post to publish for train run {}", this.trainRun);

            return;
        }

        Post post = optionalPost.get();

        this.multiplatformPublisher.publish(post);
    }
}
