package org.moera.node.operations;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.data.Draft;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.Posting;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.media.DirectServeOperations.DirectServePath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WriteMediaAttachmentsTest {

    private DirectServeOperations directServeOperations;
    private Posting parent;
    private EntryRevision revision;
    private MediaFileOwner owner;
    private RemoteMediaFile remote;

    @BeforeEach
    void setUp() {
        directServeOperations = mock(DirectServeOperations.class);
        when(directServeOperations.directPath(any(), any(), any())).thenReturn(DirectServePath.NONE);
        when(directServeOperations.directDownloadPath(any(), any(), any())).thenReturn(DirectServePath.NONE);
        parent = new Posting();
        parent.setId(UUID.randomUUID());
        revision = revision();
        var file = new MediaFile();
        file.setId("media-hash");
        file.setMimeType("image/png");
        owner = new MediaFileOwner();
        owner.setId(UUID.randomUUID());
        owner.setMediaFile(file);
        remote = new RemoteMediaFile();
        remote.setId(UUID.randomUUID());
        remote.setNodeName("remote");
        remote.setMediaId("remote-media");
    }

    @Test
    void knownIdsBuildTheCurrentInMemoryAttachmentsWithoutUsingTheCacheOrPostingsCollections() {
        var ids = new MediaPostingIds(parent.getId());
        var localChild = child(true);
        var remoteChild = child(false);
        ids.add(localChild);
        ids.add(remoteChild);
        revision.setAttachmentsCache("obsolete cache");
        var local = new EntryAttachment(revision, owner, 1);
        local.setEmbedded(false);
        revision.addAttachment(local);
        revision.addAttachment(new EntryAttachment(revision, null, remote, 0));

        var media = MediaAttachmentsProvider.relations(directServeOperations, ids).getMediaAttachments(revision, null);

        assertEquals(List.of(remoteChild.getId().toString(), localChild.getId().toString()),
            media.stream().map(MediaAttachment::getPostingId).toList());
        assertFalse(media.getLast().isEmbedded());
    }

    @Test
    void restoredRevisionWithDifferentAttachmentIdsUsesTheSameMediaPostingIds() {
        var ids = new MediaPostingIds(parent.getId());
        var child = child(true);
        ids.add(child);
        revision.addAttachment(new EntryAttachment(revision, owner, 0));
        var restored = revision();
        restored.addAttachment(new EntryAttachment(restored, owner, 0));
        var provider = MediaAttachmentsProvider.relations(directServeOperations, ids);

        assertEquals(child.getId().toString(), provider.getMediaAttachments(revision, null).getFirst().getPostingId());
        assertEquals(child.getId().toString(), provider.getMediaAttachments(restored, null).getFirst().getPostingId());
    }

    @Test
    void downloadedMediaUsesLocalIdentityEvenWhenLocalAndRemoteIdsCoincide() {
        remote.setId(owner.getId());
        var ids = new MediaPostingIds(parent.getId());
        ids.add(child(false));
        var attachment = new EntryAttachment(revision, owner, remote, 0);
        assertNull(ids.find(attachment));
        var localChild = child(true);
        ids.add(localChild);
        assertEquals(localChild.getId(), ids.find(attachment));
    }

    @Test
    void invalidRemoteMediaIsHiddenInRevisionResponsesAndRetainedInDraftResponses() {
        remote.setInvalid(true);
        revision.addAttachment(new EntryAttachment(revision, null, remote, 0));
        assertTrue(MediaAttachmentsProvider.relations(directServeOperations, new MediaPostingIds(parent.getId()))
            .getMediaAttachments(revision, null).isEmpty());

        var draft = new Draft();
        draft.setId(UUID.randomUUID());
        var attachment = new EntryAttachment(draft, null, 0);
        attachment.setRemoteMediaFile(remote);
        draft.addAttachment(attachment);
        var ids = new MediaPostingIds(null);
        var child = child(false);
        child.setParentMediaEntry(null);
        ids.add(child);
        var media = DraftAttachmentsProvider.relations(directServeOperations, ids).getMediaAttachments(draft, null);

        assertEquals(1, media.size());
        assertEquals(child.getId().toString(), media.getFirst().getPostingId());
    }

    private EntryRevision revision() {
        var result = new EntryRevision();
        result.setId(UUID.randomUUID());
        result.setEntry(parent);
        return result;
    }

    private Posting child(boolean local) {
        var posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setParentMediaEntry(parent);
        if (local) {
            posting.setParentMedia(owner);
        } else {
            posting.setParentRemoteMedia(remote);
        }
        return posting;
    }

}
