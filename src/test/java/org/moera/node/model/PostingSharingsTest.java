package org.moera.node.model;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PostingOperations;
import org.moera.lib.node.types.PostingText;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.media.DirectServeOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PostingSharingsTest {

    @Test
    void missingCounterOnPickedPostingMeansZero() {
        PostingInfo info = new PostingInfo();
        info.setId("remote");
        info.setTotalComments(0);
        Posting posting = new Posting();
        posting.setTotalSharings(10);

        PostingInfoUtil.toPickedPosting(info, posting, null);

        assertEquals(0, posting.getTotalSharings());
        assertEquals(Principal.PUBLIC, posting.getReceiverViewTotalSharingsPrincipal());
        assertEquals(Principal.PUBLIC, posting.getReceiverViewSharingsPrincipal());
    }

    @Test
    void listAndCounterPermissionsAreIndependentAndPersisted() {
        PostingText text = new PostingText();
        PostingOperations permissions = new PostingOperations();
        permissions.setViewSharings(Principal.PRIVATE);
        permissions.setViewTotalSharings(Principal.PUBLIC);
        text.setOperations(permissions);
        Posting posting = posting();
        assertFalse(PostingTextUtil.sameAsEntry(text, posting));

        PostingTextUtil.toEntry(text, posting);

        assertTrue(PostingTextUtil.sameAsEntry(text, posting));
        assertEquals(Principal.PRIVATE, posting.getViewSharingsPrincipal());
        assertEquals(5, info(posting).getTotalSharings());
        posting.setViewTotalSharingsPrincipal(Principal.PRIVATE);
        assertEquals(0, info(posting).getTotalSharings());
    }

    @Test
    void pickedCounterUsesOriginalPermissions() {
        Posting posting = posting();
        posting.setReceiverName("remote");
        posting.setViewTotalSharingsPrincipal(Principal.PUBLIC);
        posting.setReceiverViewTotalSharingsPrincipal(Principal.PRIVATE);
        assertEquals(0, info(posting).getTotalSharings());
        posting.setViewTotalSharingsPrincipal(Principal.PRIVATE);
        posting.setReceiverViewTotalSharingsPrincipal(Principal.PUBLIC);
        assertEquals(5, info(posting).getTotalSharings());
    }

    private static PostingInfo info(Posting posting) {
        return PostingInfoUtil.build(posting, false, AccessCheckers.PUBLIC, null, mock(DirectServeOperations.class));
    }

    private static Posting posting() {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setTotalSharings(5);
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setBody("{\"text\":\"posting\"}");
        revision.setBodyPreview("{\"text\":\"posting\"}");
        posting.setCurrentRevision(revision);
        return posting;
    }

}
