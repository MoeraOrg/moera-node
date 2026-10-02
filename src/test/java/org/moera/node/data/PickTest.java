package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.moera.lib.node.types.principal.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickTest {

    @Test
    void redirectPreservesPublicationAndOriginalSourceAcrossMultipleHops() {
        Pick pick = new Pick();
        pick.setId(UUID.randomUUID());
        pick.setNodeId(UUID.randomUUID());
        pick.setRemoteNodeName("original-node");
        pick.setRemotePostingId("original-posting");
        pick.setFeedName("news");
        pick.setRemoteFeedName("timeline");
        pick.setRecommended(true);
        pick.setPinned(true);
        pick.setViewed(true);
        pick.setRead(true);
        pick.setSatisfied(true);
        pick.setPostingViewPrincipal(Principal.PRIVATE);
        pick.setAllowMultiple(true);
        pick.setPublishAt(Timestamp.valueOf("2026-10-11 12:00:00"));

        Pick redirected = pick.redirectTo("next-node", "next-posting");
        Pick finalPick = redirected.redirectTo("final-node", "final-posting");

        assertNotSame(pick, redirected);
        assertNotSame(redirected, finalPick);
        assertEquals("original-node", pick.getRemoteNodeName());
        assertEquals("original-posting", pick.getRemotePostingId());
        assertNull(pick.getSourceNodeName());
        assertNull(pick.getSourcePostingId());
        assertEquals("next-node", redirected.getRemoteNodeName());
        assertEquals("next-posting", redirected.getRemotePostingId());
        assertEquals("final-node", finalPick.getRemoteNodeName());
        assertEquals("final-posting", finalPick.getRemotePostingId());
        assertEquals("original-node", finalPick.getSourceNodeName());
        assertEquals("original-posting", finalPick.getSourcePostingId());
        assertEquals("news", finalPick.getFeedName());
        assertEquals("timeline", finalPick.getRemoteFeedName());
        assertTrue(finalPick.isRecommended());
        assertTrue(finalPick.isPinned());
        assertTrue(finalPick.isViewed());
        assertTrue(finalPick.isRead());
        assertTrue(finalPick.isSatisfied());
        assertEquals(Principal.PRIVATE, finalPick.getPostingViewPrincipal());
        assertTrue(finalPick.isAllowMultiple());
        assertEquals(Timestamp.valueOf("2026-10-11 12:00:00"), finalPick.getPublishAt());
        assertNull(finalPick.getId());
        assertNull(finalPick.getNodeId());
    }

    @ParameterizedTest
    @CsvSource({
        "original-node, original-posting, original-node, original-posting",
        "original-node, , original-node, current-posting",
        ", original-posting, current-node, original-posting",
        ", , current-node, current-posting",
        "'', '', '', ''"
    })
    void redirectFillsOnlyMissingSourceFields(
        String sourceNodeName, String sourcePostingId, String expectedSourceNodeName, String expectedSourcePostingId
    ) {
        Pick pick = new Pick();
        pick.setRemoteNodeName("current-node");
        pick.setRemotePostingId("current-posting");
        pick.setSourceNodeName(sourceNodeName);
        pick.setSourcePostingId(sourcePostingId);

        Pick redirected = pick.redirectTo("next-node", "next-posting");

        assertEquals(expectedSourceNodeName, redirected.getSourceNodeName());
        assertEquals(expectedSourcePostingId, redirected.getSourcePostingId());
        assertEquals(sourceNodeName, pick.getSourceNodeName());
        assertEquals(sourcePostingId, pick.getSourcePostingId());
    }

}
