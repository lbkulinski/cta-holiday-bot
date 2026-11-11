package app.cta4j.common.config;

import app.cta4j.common.dto.Post;
import app.cta4j.common.publisher.MultiplatformPublisher;
import app.cta4j.common.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
@Slf4j
public class FunctionConfig {
    @Bean
    public Supplier<String> socialPublisher(
        PostService postService,
        MultiplatformPublisher multiplatformPublisher,
        @Value("${app.cta.train.run}") String trainRun
    ) {
        return () -> {
            Post post = postService.buildPost(trainRun);

            if (post == null) {
                log.info("No post to publish for train run {}", trainRun);

                return "NO_POST";
            }

            multiplatformPublisher.publish(post);

            return "OK";
        };
    }
}
