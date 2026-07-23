package org.moera.node.operations;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.media.MediaGrantSupplier;
import org.moera.node.model.MediaAttachmentUtil;

public interface MediaAttachmentsProvider {

    MediaAttachmentsProvider NONE = (revision, grantSupplier) -> Collections.emptyList();

    static MediaAttachmentsProvider relations(DirectServeOperations directServe) {
        return (revision, grantSupplier) ->
            revision.getAttachments().stream()
                .filter(MediaAttachmentUtil::isVisible)
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> MediaAttachmentUtil.build(ea, directServe, grantSupplier))
                .collect(Collectors.toList());
    }

    List<MediaAttachment> getMediaAttachments(EntryRevision revision, MediaGrantSupplier grantSupplier);

}
