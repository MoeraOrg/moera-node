package org.moera.node.operations;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.moera.lib.node.types.body.Body;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.LinkedEntry;
import org.moera.node.data.LinkedEntryRepository;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.picker.PickerPool;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LinkedEntryOperationsTest {

    private final LinkedEntryOperations operations = new LinkedEntryOperations();
    private final LinkedEntryRepository linkedEntryRepository = mock(LinkedEntryRepository.class);
    private final PostingRepository postingRepository = mock(PostingRepository.class);
    private final PickerPool pickerPool = mock(PickerPool.class);
    private final UUID nodeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        UniversalContext context = mock(UniversalContext.class);
        when(context.nodeName()).thenReturn("local");
        when(context.nodeId()).thenReturn(nodeId);
        when(linkedEntryRepository.save(any(LinkedEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(operations, "universalContext", context);
        ReflectionTestUtils.setField(operations, "linkedEntryRepository", linkedEntryRepository);
        ReflectionTestUtils.setField(operations, "postingRepository", postingRepository);
        ReflectionTestUtils.setField(operations, "pickerPool", pickerPool);
    }

    @Test
    void localLinkResolvesWithinTheCurrentNodeAndDuplicatesAreStoredOnce() {
        UUID targetId = UUID.randomUUID();
        Posting target = new Posting();
        target.setId(targetId);
        when(postingRepository.findByNodeIdAndId(nodeId, targetId)).thenReturn(Optional.of(target));
        EntryRevision revision = revision("local", targetId.toString());
        Body body = new Body(revision.getBody());
        body.setText(body.getText() + body.getText());
        revision.setBody(body.getEncoded());

        operations.create(revision);

        assertEquals(1, revision.getLinkedEntries().size());
        LinkedEntry link = revision.getLinkedEntries().iterator().next();
        assertSame(target, link.getLinkedEntry());
        assertEquals("local_0", link.getReceiverNodeName());
        assertEquals(targetId.toString(), link.getReceiverEntryId());
        assertSame(revision, link.getEntryRevision());
        verifyNoInteractions(pickerPool);
    }

    @Test
    void remoteLinkIsStoredAndSubmittedToPicker() {
        EntryRevision revision = revision("remote", "target");

        operations.create(revision);

        LinkedEntry link = revision.getLinkedEntries().iterator().next();
        assertEquals("remote_0", link.getReceiverNodeName());
        assertEquals("target", link.getReceiverEntryId());
        assertNull(link.getLinkedEntry());
        ArgumentCaptor<Pick> pickCaptor = ArgumentCaptor.forClass(Pick.class);
        verify(pickerPool).pickAfterCommit(pickCaptor.capture());
        Pick pick = pickCaptor.getValue();
        assertEquals(nodeId, pick.getNodeId());
        assertEquals("remote_0", pick.getRemoteNodeName());
        assertEquals("target", pick.getRemotePostingId());
        assertSame(link, pick.getLinkedEntry());
        assertNull(pick.getFeedName());
    }

    @Test
    void existingRemoteCopyIsReusedWithoutAnotherDownload() {
        Posting target = new Posting();
        when(postingRepository.findByReceiverId(nodeId, "remote_0", "target")).thenReturn(Optional.of(target));
        EntryRevision revision = revision("remote", "target");

        operations.create(revision);

        assertSame(target, revision.getLinkedEntries().iterator().next().getLinkedEntry());
        verifyNoInteractions(pickerPool);
    }

    @Test
    void mediaPostingsDoNotStoreOrDownloadReferences() {
        EntryRevision revision = revision("remote", "target");
        revision.getEntry().setParentMediaEntry(new Posting());

        operations.create(revision);

        assertTrue(revision.getLinkedEntries().isEmpty());
        verifyNoInteractions(linkedEntryRepository, postingRepository, pickerPool);
    }

    @Test
    void missingLocalPostingDoesNotStartARemoteDownload() {
        EntryRevision revision = revision("local", "invalid-id");

        operations.create(revision);

        LinkedEntry link = revision.getLinkedEntries().iterator().next();
        assertNull(link.getLinkedEntry());
        verifyNoInteractions(pickerPool);
    }

    @Test
    void mediaPostingWithoutAParentEntryDoesNotStoreReferences() {
        EntryRevision revision = revision("remote", "target");
        revision.getEntry().setParentMedia(new MediaFileOwner());

        operations.create(revision);

        assertTrue(revision.getLinkedEntries().isEmpty());
        verifyNoInteractions(linkedEntryRepository, postingRepository, pickerPool);
    }

    @Test
    void redirectedPickKeepsTheLinkAndResolvesToTheOriginalCopy() {
        LinkedEntry link = new LinkedEntry();
        link.setId(UUID.randomUUID());
        when(linkedEntryRepository.findByNodeIdAndId(nodeId, link.getId())).thenReturn(Optional.of(link));
        Pick pick = new Pick();
        pick.setNodeId(nodeId);
        pick.setLinkedEntry(link);
        pick.setRemoteNodeName("relay");
        pick.setRemotePostingId("copy");
        Pick redirected = pick.redirectTo("original", "original-posting");
        Posting target = new Posting();
        target.setNodeId(nodeId);

        operations.picked(redirected, target);

        assertSame(link, redirected.getLinkedEntry());
        assertSame(target, link.getLinkedEntry());
    }

    private EntryRevision revision(String nodeName, String postingId) {
        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(nodeId);
        EntryRevision revision = new EntryRevision();
        revision.setId(UUID.randomUUID());
        revision.setEntry(posting);
        Body body = new Body();
        body.setText("<a href=\"https://moera.page/@" + nodeName + "/~/post/" + postingId + "\">post</a>");
        revision.setBody(body.getEncoded());
        posting.setCurrentRevision(revision);
        return revision;
    }

}
