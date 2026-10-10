package org.moera.node.picker;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.moera.lib.node.MoeraNode;
import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.body.Body;
import org.moera.node.api.node.NodeApi;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.media.MediaOperations;
import org.moera.node.operations.ReactionTotalOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.option.Options;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.moera.node.util.MediaTransactionTestSupport.assertOutsideTransaction;
import static org.moera.node.util.MediaTransactionTestSupport.transaction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PickerTest {

    @Test
    void fetchingPostingDoesNotHoldTransactionOrConnection() throws Exception {
        NodeApi nodeApi = mock(NodeApi.class);
        MoeraNode remote = mock(MoeraNode.class);
        when(nodeApi.at("remote", "carte")).thenReturn(remote);
        IllegalStateException failure = new IllegalStateException("Download interrupted");
        when(remote.getPosting("posting", false)).thenAnswer(invocation -> {
            assertOutsideTransaction();
            throw failure;
        });
        Picker picker = new Picker(mock(PickerPool.class), "remote") {
            @Override
            protected String generateCarte(String targetNodeName, Scope clientScope) {
                return "carte";
            }
        };
        ReflectionTestUtils.setField(picker, "nodeApi", nodeApi);
        ReflectionTestUtils.setField(picker, "tx", transaction());
        Pick pick = new Pick();
        pick.setRemotePostingId("posting");

        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> ReflectionTestUtils.invokeMethod(picker, "download", pick)));
        assertOutsideTransaction();
    }

    @Test
    void requestedPublicationKeepsAllStoryAttributes() {
        Posting posting = posting(UUID.randomUUID());
        StoryOperations storyOperations = mock(StoryOperations.class);
        Picker picker = picker(posting, storyOperations);
        Timestamp publishAt = Timestamp.from(Instant.ofEpochSecond(1000));
        String[] externalSourceUri = {"https://one.example/story", "https://two.example/story"};

        savePosting(picker, postingInfo(), "news", true, false, true, true, true, publishAt, externalSourceUri);

        var publications = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(storyOperations).publish(eq(posting), publications.capture(), eq(true));
        StoryAttributes publication = (StoryAttributes) publications.getValue().getFirst();
        assertEquals("news", publication.getFeedName());
        assertEquals(1000L, publication.getPublishAt());
        assertTrue(publication.getPinned());
        assertFalse(publication.getViewed());
        assertTrue(publication.getRead());
        assertTrue(publication.getSatisfied());
        assertEquals(
            List.of("https://one.example/story", "https://two.example/story"),
            publication.getExternalSourceUri()
        );
    }

    @Test
    void downloadWithoutPublicationPassesNullFeedToStoryOperations() {
        Posting posting = posting(UUID.randomUUID());
        StoryOperations storyOperations = mock(StoryOperations.class);
        Picker picker = picker(posting, storyOperations);

        savePosting(picker, postingInfo(), null, false, false, false, false, false, null, new String[0]);

        var publications = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(storyOperations).publish(eq(posting), publications.capture(), eq(false));
        StoryAttributes publication = (StoryAttributes) publications.getValue().getFirst();
        assertNull(publication.getFeedName());
    }

    @Test
    void updatedPostingIsPublishedInRequestedFeed() {
        UUID nodeId = UUID.randomUUID();
        Posting posting = posting(nodeId);
        PostingInfo postingInfo = postingInfo();
        StoryOperations storyOperations = mock(StoryOperations.class);
        Picker picker = picker(posting, storyOperations);

        savePosting(picker, postingInfo, "news", false, false, false, false, false, null, new String[0]);

        verify(storyOperations).publish(eq(posting), any(), eq(false));
    }

    @Test
    void unchangedDeletedPostingIsRestoredBeforePublication() {
        UUID nodeId = UUID.randomUUID();
        Posting posting = posting(nodeId);
        Timestamp deletedAt = Timestamp.from(Instant.ofEpochSecond(1100));
        posting.setDeletedAt(deletedAt);
        posting.getCurrentRevision().setDeletedAt(deletedAt);
        PostingInfo postingInfo = postingInfo();
        postingInfo.setEditedAt(1000L);
        StoryOperations storyOperations = mock(StoryOperations.class);
        Picker picker = picker(posting, storyOperations);

        savePosting(picker, postingInfo, "news", false, false, false, false, false, null, new String[0]);

        assertNull(posting.getDeletedAt());
        assertNull(posting.getCurrentRevision().getDeletedAt());
        verify((UniversalContext) ReflectionTestUtils.getField(picker, "universalContext"))
            .send(any(PostingRestoredLiberin.class));
        verify(storyOperations).publish(eq(posting), any(), eq(false));
    }

    private static Posting posting(UUID nodeId) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(nodeId);
        posting.setReceiverName("remote");
        posting.setReceiverEntryId("posting");
        posting.setEditedAt(Timestamp.from(Instant.ofEpochSecond(1000)));
        posting.setOwnerName("owner");
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(posting);
        revision.setReceiverRevisionId("revision");
        posting.setCurrentRevision(revision);
        posting.setCurrentReceiverRevisionId("revision");
        return posting;
    }

    private static PostingInfo postingInfo() {
        PostingInfo postingInfo = new PostingInfo();
        postingInfo.setId("posting");
        postingInfo.setRevisionId("revision");
        postingInfo.setOwnerName("owner");
        postingInfo.setTotalComments(0);
        postingInfo.setCreatedAt(1000L);
        postingInfo.setEditedAt(2000L);
        postingInfo.setRevisionCreatedAt(2000L);
        postingInfo.setBodyPreview(new Body(Body.EMPTY));
        postingInfo.setBodySrcFormat(SourceFormat.MARKDOWN);
        postingInfo.setBodySrcHash(new byte[0]);
        postingInfo.setBody(new Body(Body.EMPTY));
        postingInfo.setBodyFormat(BodyFormat.MESSAGE);
        postingInfo.setHeading("");
        postingInfo.setDescription("");
        postingInfo.setReactions(new ReactionTotalsInfo());
        return postingInfo;
    }

    private static Picker picker(Posting posting, StoryOperations storyOperations) {
        Picker picker = new Picker(mock(PickerPool.class), "remote");
        picker.setNodeId(posting.getNodeId());
        PostingRepository postingRepository = mock(PostingRepository.class);
        when(postingRepository.findByReceiverId(posting.getNodeId(), "remote", "posting"))
            .thenReturn(java.util.Optional.of(posting));
        when(postingRepository.findByReceiverIdForUpdate(posting.getNodeId(), "remote", "posting"))
            .thenReturn(java.util.Optional.of(posting));
        when(postingRepository.saveAndFlush(posting)).thenReturn(posting);
        ReflectionTestUtils.setField(picker, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(picker, "reactionTotalRepository", mock(ReactionTotalRepository.class));
        ReactionTotalOperations reactionTotalOperations = mock(ReactionTotalOperations.class);
        when(reactionTotalOperations.isSame(any(), any())).thenReturn(true);
        ReflectionTestUtils.setField(picker, "reactionTotalOperations", reactionTotalOperations);
        ReflectionTestUtils.setField(picker, "mediaOperations", mock(MediaOperations.class));
        ReflectionTestUtils.setField(picker, "directServeOperations", mock(DirectServeOperations.class));
        UniversalContext universalContext = mock(UniversalContext.class);
        when(universalContext.getOptions()).thenReturn(mock(Options.class));
        ReflectionTestUtils.setField(picker, "universalContext", universalContext);
        ReflectionTestUtils.setField(picker, "storyOperations", storyOperations);
        return picker;
    }

    private static void savePosting(
        Picker picker,
        PostingInfo postingInfo,
        String feedName,
        boolean pinned,
        boolean viewed,
        boolean read,
        boolean satisfied,
        boolean allowMultiple,
        Timestamp publishAt,
        String[] externalSourceUri
    ) {
        Object snapshot = ReflectionTestUtils.invokeMethod(picker, "snapshot", postingInfo, "posting");
        ReflectionTestUtils.invokeMethod(
            picker,
            "savePosting",
            postingInfo,
            null,
            null,
            snapshot,
            List.of(),
            "posting",
            feedName,
            null,
            null,
            null,
            false,
            pinned,
            viewed,
            read,
            satisfied,
            null,
            allowMultiple,
            publishAt,
            externalSourceUri,
            new ArrayList<Pick>()
        );
    }

}
