package org.moera.node.operations;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentPostingId;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.media.grant.MediaGrantSupplier;
import org.moera.node.model.MediaAttachmentUtil;

public interface MediaAttachmentsProvider {

    MediaAttachmentsProvider NONE = (revision, grantSupplier) -> Collections.emptyList();

    static MediaAttachmentsProvider relations(DirectServeOperations directServe, MediaPostingIds postingIds) {
        return (revision, grantSupplier) -> revision.getAttachments().stream()
            .filter(MediaAttachmentUtil::isVisible)
            .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
            .map(ea -> MediaAttachmentUtil.build(ea, postingIds.find(ea), directServe, grantSupplier))
            .collect(Collectors.toList());
    }

    static MediaAttachmentsProvider relations(DirectServeOperations directServe) {
        return (revision, grantSupplier) ->
            revision.getAttachments().stream()
                .filter(MediaAttachmentUtil::isVisible)
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> MediaAttachmentUtil.build(ea, directServe, grantSupplier))
                .collect(Collectors.toList());
    }

    static MediaAttachmentsProvider database(
        EntryAttachmentRepository entryAttachmentRepository, DirectServeOperations directServe
    ) {
        return (revision, grantSupplier) -> database(
            entryAttachmentRepository, directServe, List.of(revision.getId())
        ).getMediaAttachments(revision, grantSupplier);
    }

    static MediaAttachmentsProvider database(
        EntryAttachmentRepository entryAttachmentRepository,
        DirectServeOperations directServe,
        Collection<UUID> revisionIds
    ) {
        if (revisionIds.isEmpty()) {
            return NONE;
        }

        var attachments = entryAttachmentRepository.findByEntryRevisions(revisionIds);
        if (attachments.isEmpty()) {
            return NONE;
        }

        var byRevision = attachments.stream()
            .collect(Collectors.groupingBy(ea -> ea.getEntryRevision().getId()));
        var postingIds = entryAttachmentRepository.findPostingIdsByEntryRevisions(byRevision.keySet()).stream()
            .collect(Collectors.toMap(
                EntryAttachmentPostingId::attachmentId,
                EntryAttachmentPostingId::postingId,
                (first, duplicate) -> first
            ));

        return (revision, grantSupplier) ->
            byRevision.getOrDefault(revision.getId(), Collections.emptyList()).stream()
                .filter(MediaAttachmentUtil::isVisible)
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> MediaAttachmentUtil.build(ea, postingIds.get(ea.getId()), directServe, grantSupplier))
                .collect(Collectors.toList());
    }

    List<MediaAttachment> getMediaAttachments(EntryRevision revision, MediaGrantSupplier grantSupplier);

}
