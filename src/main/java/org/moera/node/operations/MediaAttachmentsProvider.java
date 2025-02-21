package org.moera.node.operations;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.model.MediaAttachmentUtil;

public interface MediaAttachmentsProvider {

    MediaAttachmentsProvider NONE = (revision, receiverName) -> Collections.emptyList();
    MediaAttachmentsProvider RELATIONS =
        (revision, receiverName) ->
            revision.getAttachments().stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> MediaAttachmentUtil.build(ea, receiverName))
                .collect(Collectors.toList());

    List<MediaAttachment> getMediaAttachments(EntryRevision revision, String receiverName);

}
