package org.moera.node.liberin.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class LiberinSourceUriTest {

    @Test
    void includesAllOwnerSourceUrisInModel() {
        var liberin = new ForeignCommentAddedLiberin(
            "remote.example",
            "posting-owner",
            "Posting Owner",
            "https://source.example/posting-owner",
            null,
            null,
            "posting-id",
            null,
            null,
            null,
            "comment-owner",
            "Comment Owner",
            "https://source.example/comment-owner",
            null,
            null,
            "comment-id",
            null,
            null,
            null
        );

        Map<String, Object> model = liberin.getModel(null);

        assertThat(model.get("postingOwnerSourceUri")).isEqualTo("https://source.example/posting-owner");
        assertThat(model.get("commentOwnerSourceUri")).isEqualTo("https://source.example/comment-owner");
    }

}
