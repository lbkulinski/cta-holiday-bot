package app.cta4j.common.config;

import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.MultiplatformPublisher;
import app.cta4j.common.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.function.Function;

@Configuration
public class FunctionConfiguration {
    private static final Logger log = LoggerFactory.getLogger(FunctionConfiguration.class);

    private final PostService postService;
    private final MultiplatformPublisher multiplatformPublisher;

    private final String trainRun;

    @Autowired
    public FunctionConfiguration(
        PostService postService,
        MultiplatformPublisher multiplatformPublisher,
        @Value("${app.cta.train.run}") String trainRun
    ) {
        this.postService = postService;
        this.multiplatformPublisher = multiplatformPublisher;
        this.trainRun = trainRun;
    }

    @Bean
    public Function<Void, String> socialPublisher() {
        return event -> {
            Optional<Post> optionalPost = this.postService.buildPost(this.trainRun);

            if (optionalPost.isEmpty()) {
                log.info("No post to publish for train run {}", this.trainRun);

                return "NO_POST";
            }

            Post post = optionalPost.get();

            this.multiplatformPublisher.publish(post);

            return "OK";
        };
    }
}
