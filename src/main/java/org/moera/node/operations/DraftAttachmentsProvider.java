package org.moera.node.operations;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.data.Draft;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentPostingId;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.media.grant.MediaGrantSupplier;
import org.moera.node.model.MediaAttachmentUtil;

public interface DraftAttachmentsProvider {

    static DraftAttachmentsProvider relations(DirectServeOperations directServe, MediaPostingIds postingIds) {
        return (draft, grantSupplier) -> draft.getAttachments().stream()
            .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
            .map(ea -> MediaAttachmentUtil.build(ea, postingIds.find(ea), directServe, grantSupplier))
            .collect(Collectors.toList());
    }

    static DraftAttachmentsProvider database(
        EntryAttachmentRepository entryAttachmentRepository,
        DirectServeOperations directServe,
        Collection<Draft> drafts
    ) {
        if (drafts.isEmpty()) {
            return (draft, grantSupplier) -> Collections.emptyList();
        }

        var draftIds = drafts.stream().map(Draft::getId).collect(Collectors.toSet());
        var attachments = entryAttachmentRepository.findByDrafts(draftIds);
        if (attachments.isEmpty()) {
            return (draft, grantSupplier) -> Collections.emptyList();
        }

        var byDraft = attachments.stream().collect(Collectors.groupingBy(ea -> ea.getDraft().getId()));
        var postingIds = entryAttachmentRepository.findPostingIdsByDrafts(byDraft.keySet()).stream()
            .collect(Collectors.toMap(
                EntryAttachmentPostingId::attachmentId,
                EntryAttachmentPostingId::postingId,
                (first, duplicate) -> first
            ));

        return (draft, grantSupplier) -> byDraft.getOrDefault(draft.getId(), Collections.emptyList()).stream()
            .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
            .map(ea -> MediaAttachmentUtil.build(ea, postingIds.get(ea.getId()), directServe, grantSupplier))
            .collect(Collectors.toList());
    }

    List<MediaAttachment> getMediaAttachments(Draft draft, MediaGrantSupplier grantSupplier);

}
