package com.cta4j.twitter.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TwitterDtoTest {
    @Test
    void tweetMediaRejectsEmptyMediaIds() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new CreateTweetRequest.Media(List.of()))
            .withMessage("mediaIds must not be empty");
    }

    @Test
    void tweetMediaMakesDefensiveCopy() {
        var ids = new ArrayList<>(List.of("1"));

        var media = new CreateTweetRequest.Media(ids);

        ids.add("2");

        assertThat(media.mediaIds()).containsExactly("1");
    }

    @Test
    void requestsRejectNulls() {
        assertThatNullPointerException().isThrownBy(() -> new CreateTweetRequest(null, null));
        assertThatNullPointerException().isThrownBy(() -> new CreateTweetRequest.Media(null));
        assertThatNullPointerException().isThrownBy(() -> new UploadMediaRequest(null, "cat"));
        assertThatNullPointerException().isThrownBy(() -> new UploadMediaRequest("media", null));
        assertThatNullPointerException().isThrownBy(() -> new CreateMediaMetadataRequest.AltText(null));
        assertThatNullPointerException().isThrownBy(() -> new CreateMediaMetadataRequest.Metadata(null));
        assertThatNullPointerException().isThrownBy(() -> new CreateMediaMetadataRequest(null,
            new CreateMediaMetadataRequest.Metadata(new CreateMediaMetadataRequest.AltText("a"))));
        assertThatNullPointerException().isThrownBy(() -> new CreateMediaMetadataRequest("id", null));
    }

    @Test
    void responsesRejectNulls() {
        assertThatNullPointerException().isThrownBy(() -> new Tweet(null, "text"));
        assertThatNullPointerException().isThrownBy(() -> new Tweet("id", null));
        assertThatNullPointerException().isThrownBy(() -> new Media(null));
        assertThatNullPointerException().isThrownBy(() -> new MediaMetadata(null));
        assertThatNullPointerException().isThrownBy(() -> new CreateTweetResponse(null));
        assertThatNullPointerException().isThrownBy(() -> new UploadMediaResponse(null));
        assertThatNullPointerException().isThrownBy(() -> new CreateMediaMetadataResponse(null));
    }
}
