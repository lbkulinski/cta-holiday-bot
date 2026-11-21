package com.cta4j.mastodon.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MastodonDtoTest {
    @Test
    void createStatusRequestAllowsNullMediaIds() {
        assertThat(new CreateStatusRequest("hi", null).mediaIds()).isNull();
    }

    @Test
    void createStatusRequestMakesDefensiveCopy() {
        var ids = new ArrayList<>(List.of("1"));

        var request = new CreateStatusRequest("hi", ids);

        ids.add("2");

        assertThat(request.mediaIds()).containsExactly("1");
    }

    @Test
    void dtosRejectNulls() {
        assertThatNullPointerException().isThrownBy(() -> new CreateStatusRequest(null, null));
        assertThatNullPointerException().isThrownBy(() -> new Status(null));
        assertThatNullPointerException().isThrownBy(() -> new Media(null));
    }
}
