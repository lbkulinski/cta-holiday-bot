package com.cta4j.post.model;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class PostTest {
    @Test
    void createsPostWithoutImage() {
        var post = new Post("hello", null);

        assertThat(post.text()).isEqualTo("hello");
        assertThat(post.image()).isNull();
    }

    @Test
    void createsPostWithImage() {
        var image = new Post.Image(new File("map.png"), "alt");

        var post = new Post("hello", image);

        assertThat(post.image()).isSameAs(image);
    }

    @Test
    void rejectsNullText() {
        assertThatNullPointerException().isThrownBy(() -> new Post(null, null));
    }

    @Test
    void imageRejectsNullFile() {
        assertThatNullPointerException().isThrownBy(() -> new Post.Image(null, "alt"));
    }

    @Test
    void imageRejectsNullAltText() {
        assertThatNullPointerException().isThrownBy(() -> new Post.Image(new File("map.png"), null));
    }
}
