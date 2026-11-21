package com.cta4j.bluesky.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class BlueskyDtoTest {
    private static final Blob BLOB = new Blob("blob", new Blob.Ref("link"), "image/png", 1L);
    private static final ImageBlob IMAGE = new ImageBlob(BLOB, "alt");

    @Test
    void blobAcceptsZeroSize() {
        assertThat(new Blob("blob", new Blob.Ref("link"), "image/png", 0L).size()).isZero();
    }

    @Test
    void blobRejectsNegativeSize() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Blob("blob", new Blob.Ref("link"), "image/png", -1L))
            .withMessage("size must be non-negative");
    }

    @Test
    void blobRejectsNulls() {
        var ref = new Blob.Ref("link");

        assertThatNullPointerException().isThrownBy(() -> new Blob(null, ref, "image/png", 1L));
        assertThatNullPointerException().isThrownBy(() -> new Blob("blob", null, "image/png", 1L));
        assertThatNullPointerException().isThrownBy(() -> new Blob("blob", ref, null, 1L));
        assertThatNullPointerException().isThrownBy(() -> new Blob.Ref(null));
    }

    @Test
    void embedAcceptsOneToFourImages() {
        for (int count = 1; count <= 4; count++) {
            var embed = new CreateRecordRequest.Embed("type", java.util.Collections.nCopies(count, IMAGE));

            assertThat(embed.images()).hasSize(count);
        }
    }

    @Test
    void embedRejectsEmptyAndTooManyImages() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new CreateRecordRequest.Embed("type", List.of()))
            .withMessage("images must contain 1-4 items");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new CreateRecordRequest.Embed("type", java.util.Collections.nCopies(5, IMAGE)));
    }

    @Test
    void embedMakesDefensiveCopyOfImages() {
        var images = new ArrayList<>(List.of(IMAGE));

        var embed = new CreateRecordRequest.Embed("type", images);

        images.add(IMAGE);

        assertThat(embed.images()).hasSize(1);
    }

    @Test
    void requestRecordsRejectNulls() {
        var now = Instant.now();
        var record = new CreateRecordRequest.Record("type", "text", now, null);

        assertThatNullPointerException().isThrownBy(() -> new CreateRecordRequest.Record(null, "text", now, null));
        assertThatNullPointerException().isThrownBy(() -> new CreateRecordRequest.Record("type", null, now, null));
        assertThatNullPointerException().isThrownBy(() -> new CreateRecordRequest.Record("type", "text", null, null));
        assertThatNullPointerException().isThrownBy(() -> new CreateRecordRequest.Embed(null, List.of(IMAGE)));
        assertThatNullPointerException().isThrownBy(() -> new CreateRecordRequest.Embed("type", null));
        assertThatNullPointerException().isThrownBy(() -> new CreateRecordRequest(null, "c", record));
        assertThatNullPointerException().isThrownBy(() -> new CreateRecordRequest("r", null, record));
        assertThatNullPointerException().isThrownBy(() -> new CreateRecordRequest("r", "c", null));
    }

    @Test
    void otherDtosRejectNulls() {
        assertThatNullPointerException().isThrownBy(() -> new Session(null, "jwt"));
        assertThatNullPointerException().isThrownBy(() -> new Session("did", null));
        assertThatNullPointerException().isThrownBy(() -> new Record(null, "uri"));
        assertThatNullPointerException().isThrownBy(() -> new Record("cid", null));
        assertThatNullPointerException().isThrownBy(() -> new ImageBlob(null, "alt"));
        assertThatNullPointerException().isThrownBy(() -> new ImageBlob(BLOB, null));
        assertThatNullPointerException().isThrownBy(() -> new UploadBlobResponse(null));
        assertThatNullPointerException().isThrownBy(() -> new CreateSessionRequest(null, "pw"));
        assertThatNullPointerException().isThrownBy(() -> new CreateSessionRequest("id", null));
    }
}
