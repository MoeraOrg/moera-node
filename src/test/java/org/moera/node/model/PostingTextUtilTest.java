package org.moera.node.model;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.PostingText;
import org.moera.node.data.Entry;

public class PostingTextUtilTest {

    @Test
    void externalSourceUrisAreCopiedToEntry() {
        PostingText postingText = new PostingText();
        postingText.setExternalSourceUri(List.of("https://one.example/post", "https://two.example/post"));
        Entry entry = new Entry();

        PostingTextUtil.toEntry(postingText, entry);

        Assertions.assertArrayEquals(
            new String[]{"https://one.example/post", "https://two.example/post"},
            entry.getExternalSourceUri()
        );
    }

    @Test
    void equalExternalSourceUrisDoNotMarkPostingAsChanged() {
        PostingText postingText = new PostingText();
        postingText.setExternalSourceUri(List.of("https://one.example/post", "https://two.example/post"));
        Entry entry = new Entry();
        entry.setExternalSourceUri(new String[]{"https://one.example/post", "https://two.example/post"});

        Assertions.assertTrue(PostingTextUtil.sameAsEntry(postingText, entry));
    }

}
