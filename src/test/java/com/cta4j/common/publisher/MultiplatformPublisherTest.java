package com.cta4j.common.publisher;

import com.cta4j.post.model.Post;
import com.rollbar.notifier.Rollbar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiplatformPublisherTest {
    private final Post post = new Post("text", null);

    @Mock
    private SocialPublisher first;

    @Mock
    private SocialPublisher second;

    @Mock
    private Rollbar rollbar;

    @Test
    void publishesToEveryPublisherInOrder() {
        var publisher = new MultiplatformPublisher(List.of(this.first, this.second), this.rollbar);

        publisher.publish(this.post);

        InOrder order = inOrder(this.first, this.second);

        order.verify(this.first).publish(this.post);
        order.verify(this.second).publish(this.post);
        verifyNoInteractions(this.rollbar);
    }

    @Test
    void failureInOnePublisherDoesNotStopTheOthers() {
        var failure = new IllegalStateException("boom");

        when(this.first.getPlatformName()).thenReturn("First");
        doThrow(failure).when(this.first).publish(this.post);

        var publisher = new MultiplatformPublisher(List.of(this.first, this.second), this.rollbar);

        publisher.publish(this.post);

        verify(this.second).publish(this.post);
        verify(this.rollbar).error(failure, "Failed to publish post on First");
    }

    @Test
    void reportsEveryFailureToRollbar() {
        var firstFailure = new RuntimeException("first");
        var secondFailure = new RuntimeException("second");

        when(this.first.getPlatformName()).thenReturn("First");
        when(this.second.getPlatformName()).thenReturn("Second");
        doThrow(firstFailure).when(this.first).publish(this.post);
        doThrow(secondFailure).when(this.second).publish(this.post);

        var publisher = new MultiplatformPublisher(List.of(this.first, this.second), this.rollbar);

        publisher.publish(this.post);

        verify(this.rollbar).error(firstFailure, "Failed to publish post on First");
        verify(this.rollbar).error(secondFailure, "Failed to publish post on Second");
    }

    @Test
    void doesNothingWithNoPublishers() {
        var publisher = new MultiplatformPublisher(List.of(), this.rollbar);

        publisher.publish(this.post);

        verifyNoInteractions(this.rollbar);
    }

    @Test
    void rejectsNullPost() {
        var publisher = new MultiplatformPublisher(List.of(this.first), this.rollbar);

        assertThatNullPointerException().isThrownBy(() -> publisher.publish(null));

        verifyNoInteractions(this.first);
        verify(this.rollbar, org.mockito.Mockito.never()).error(any(Throwable.class), anyString());
    }
}
