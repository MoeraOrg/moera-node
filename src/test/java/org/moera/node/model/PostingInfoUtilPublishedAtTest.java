package org.moera.node.model;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.PostingInfo;
import org.moera.node.data.Posting;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostingInfoUtilPublishedAtTest {

    @Test
    void pickedOriginalUsesEarliestTimelinePublication() {
        PostingInfo info = postingInfo();
        info.setEarliestPublishedAt(900L);
        Posting posting = new Posting();

        PostingInfoUtil.toPickedPosting(info, posting, null);

        assertEquals(Timestamp.from(Instant.ofEpochSecond(900)), posting.getReceiverPublishedAt());
    }

    @Test
    void pickedCachedCopyUsesPublicationOfOriginal() {
        PostingInfo info = postingInfo();
        info.setReceiverName("origin");
        info.setReceiverPostingId("original-posting");
        info.setReceiverCreatedAt(1000L);
        info.setReceiverEditedAt(2000L);
        info.setEarliestPublishedAt(1500L);
        info.setReceiverPublishedAt(900L);
        Posting posting = new Posting();

        PostingInfoUtil.toPickedPosting(info, posting, null);

        assertEquals(Timestamp.from(Instant.ofEpochSecond(900)), posting.getReceiverPublishedAt());
    }

    private static PostingInfo postingInfo() {
        PostingInfo info = new PostingInfo();
        info.setId("posting");
        info.setCreatedAt(1000L);
        info.setEditedAt(2000L);
        info.setTotalComments(0);
        return info;
    }

}
