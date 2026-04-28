package org.moera.node.operations;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.model.MediaAttachmentUtil;
import org.moera.node.media.MediaGrantGenerator;

public interface MediaAttachmentsProvider {

    MediaAttachmentsProvider NONE = (revision, receiverName, grantGenerator) -> Collections.emptyList();

    static MediaAttachmentsProvider relations(DirectServeConfig config) {
        return (revision, receiverName, grantGenerator) ->
            revision.getAttachments().stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> MediaAttachmentUtil.build(ea, receiverName, config, grantGenerator))
                .collect(Collectors.toList());
    }

    List<MediaAttachment> getMediaAttachments(
        EntryRevision revision, String receiverName, MediaGrantGenerator grantGenerator
    );

}
