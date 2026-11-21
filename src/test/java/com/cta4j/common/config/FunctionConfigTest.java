package com.cta4j.common.config;

import com.cta4j.common.publisher.MultiplatformPublisher;
import com.cta4j.post.model.Post;
import com.cta4j.post.service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FunctionConfigTest {
    private static final String RUN = "417";

    private final FunctionConfig config = new FunctionConfig();

    @Mock
    private PostService postService;

    @Mock
    private MultiplatformPublisher multiplatformPublisher;

    private Supplier<String> supplier() {
        return this.config.socialPublisher(this.postService, this.multiplatformPublisher, RUN);
    }

    @Test
    void returnsNoPostAndSkipsPublishingWhenThereIsNothingToPost() {
        when(this.postService.buildPost(RUN)).thenReturn(null);

        assertThat(supplier().get()).isEqualTo("NO_POST");

        verifyNoInteractions(this.multiplatformPublisher);
    }

    @Test
    void publishesPostAndReturnsOk() {
        var post = new Post("text", null);

        when(this.postService.buildPost(RUN)).thenReturn(post);

        assertThat(supplier().get()).isEqualTo("OK");

        verify(this.multiplatformPublisher).publish(post);
    }

    @Test
    void usesConfiguredTrainRun() {
        when(this.postService.buildPost("999")).thenReturn(null);

        Supplier<String> custom = this.config.socialPublisher(this.postService, this.multiplatformPublisher, "999");

        custom.get();

        verify(this.postService).buildPost("999");
    }

    @Test
    void propagatesBuildFailures() {
        when(this.postService.buildPost(RUN)).thenThrow(new IllegalStateException("fail"));

        Supplier<String> supplier = supplier();

        assertThatThrownBy(supplier::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void propagatesPublisherFailures() {
        var post = new Post("text", null);

        when(this.postService.buildPost(RUN)).thenReturn(post);
        doThrow(new IllegalStateException("fail")).when(this.multiplatformPublisher).publish(post);

        Supplier<String> supplier = supplier();

        assertThatThrownBy(supplier::get).isInstanceOf(IllegalStateException.class);
    }
}
