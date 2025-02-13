package org.moera.node.operations;

import java.util.Comparator;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.model.MediaAttachmentUtil;

public interface MediaAttachmentsProvider {

    MediaAttachmentsProvider NONE = (revision, receiverName) -> new MediaAttachment[0];
    MediaAttachmentsProvider RELATIONS =
        (revision, receiverName) ->
            revision.getAttachments().stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> MediaAttachmentUtil.build(ea, receiverName))
                .toArray(MediaAttachment[]::new);

    MediaAttachment[] getMediaAttachments(EntryRevision revision, String receiverName);

}
