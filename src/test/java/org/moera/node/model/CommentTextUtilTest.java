package org.moera.node.model;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.CommentText;
import org.moera.node.data.Entry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentTextUtilTest {

    @Test
    void externalSourceUrisAreCopiedToEntry() {
        CommentText commentText = new CommentText();
        commentText.setExternalSourceUri(List.of("https://one.example/comment", "https://two.example/comment"));
        Entry entry = new Entry();

        CommentTextUtil.toEntry(commentText, entry);

        assertArrayEquals(
            new String[]{"https://one.example/comment", "https://two.example/comment"},
            entry.getExternalSourceUri()
        );
    }

    @Test
    void equalExternalSourceUrisDoNotMarkCommentAsChanged() {
        CommentText commentText = new CommentText();
        commentText.setExternalSourceUri(List.of("https://one.example/comment", "https://two.example/comment"));
        Entry entry = new Entry();
        entry.setExternalSourceUri(new String[]{"https://one.example/comment", "https://two.example/comment"});

        assertTrue(CommentTextUtil.sameAsEntry(commentText, entry));
    }

}
