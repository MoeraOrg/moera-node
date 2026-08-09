package org.moera.node.operations;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.types.body.Body;
import org.moera.node.data.DraftRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntryType;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.CommentHeadingUpdatedLiberin;
import org.moera.node.liberin.model.CommentMediaTextUpdatedLiberin;
import org.moera.node.liberin.model.DraftUpdatedLiberin;
import org.moera.node.liberin.model.PostingHeadingUpdatedLiberin;
import org.moera.node.liberin.model.PostingMediaTextUpdatedLiberin;
import org.moera.node.media.LocalRemoteMedia;
import org.moera.node.text.TextConverter;
import org.springframework.stereotype.Component;

@Component
public class OcrOperations {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private DraftRepository draftRepository;

    public void update(MediaFileOwner mediaFileOwner, String text) {
        entryRevisionRepository.clearAttachmentsCacheByMedia(mediaFileOwner.getId());
        updateRevisions(
            entryRevisionRepository.findByMedia(mediaFileOwner.getId()),
            mediaFileOwner.getId(),
            null,
            text
        );
        draftRepository.findByMedia(mediaFileOwner.getId())
            .forEach(draft -> universalContext.send(new DraftUpdatedLiberin(draft)));
    }

    public void updateRemote(Collection<RemoteMediaFile> remoteMediaFiles, String text) {
        // All remoteMediaFiles must have the same nodeId, nodeName, and mediaId.
        if (remoteMediaFiles.isEmpty()) {
            return;
        }

        RemoteMediaFile remoteMediaFile = remoteMediaFiles.iterator().next();
        List<UUID> ids = remoteMediaFiles.stream().map(RemoteMediaFile::getId).toList();
        entryRevisionRepository.clearAttachmentsCacheByRemoteMedia(remoteMediaFile.getNodeId(), ids);
        updateRevisions(
            entryRevisionRepository.findByRemoteMedia(remoteMediaFile.getNodeId(), ids),
            UUID.fromString(remoteMediaFile.getMediaId()),
            remoteMediaFile.getNodeName(),
            text
        );
    }

    private void updateRevisions(
        Collection<EntryRevision> revisions, UUID mediaId, String mediaNodeName, String text
    ) {
        Set<UUID> updatedEntries = new HashSet<>();
        for (EntryRevision revision : revisions) {
            Entry entry = revision.getEntry();
            boolean collapseQuotations = entry.getEntryType() == EntryType.COMMENT;
            List<LocalRemoteMedia> media = revision.getAttachments().stream()
                .map(EntryAttachment::getLocalRemoteMedia)
                .toList();
            String oldHeading = revision.getHeading();
            String oldDescription = revision.getDescription();
            TextConverter.headingToRevision(new Body(revision.getBody()), media, collapseQuotations, revision);

            if (revision.getDeletedAt() != null || !updatedEntries.add(entry.getId())) {
                continue;
            }

            switch (entry.getEntryType()) {
                case POSTING -> universalContext.send(
                    new PostingMediaTextUpdatedLiberin(entry.getId(), mediaId, mediaNodeName, null, text)
                );
                case COMMENT -> universalContext.send(
                    new CommentMediaTextUpdatedLiberin(entry.getId(), mediaId, mediaNodeName, null, text)
                );
            }
            if (
                !Objects.equals(oldHeading, revision.getHeading())
                || !Objects.equals(oldDescription, revision.getDescription())
            ) {
                switch (entry.getEntryType()) {
                    case POSTING -> universalContext.send(
                        new PostingHeadingUpdatedLiberin(
                            entry.getId(), revision.getId(), revision.getHeading(), revision.getDescription()
                        )
                    );
                    case COMMENT -> universalContext.send(
                        new CommentHeadingUpdatedLiberin(entry.getId(), revision.getId(), revision.getHeading())
                    );
                }
            }
        }
    }

}
