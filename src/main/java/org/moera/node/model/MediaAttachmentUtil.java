package org.moera.node.model;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.Posting;
import org.moera.node.media.MediaGrantSupplier;

public class MediaAttachmentUtil {

    public static MediaAttachment build(
        EntryAttachment attachment,
        DirectServeConfig config,
        MediaGrantSupplier grantSupplier
    ) {
        MediaAttachment mediaAttachment = new MediaAttachment();
        
        if (attachment.getMediaFileOwner() != null) {
            mediaAttachment.setMedia(PrivateMediaFileInfoUtil.build(
                attachment.getMediaFileOwner(), config, grantSupplier
            ));
            Posting mediaPosting = attachment.getMediaFileOwner().getPostingByParentMediaEntry(
                attachment.getEntryRevision() != null ? attachment.getEntryRevision().getEntry() : null
            );
            mediaAttachment.setPostingId(mediaPosting != null ? mediaPosting.getId().toString() : null);
        }
        if (attachment.getRemoteMediaId() != null) {
            mediaAttachment.setRemoteMedia(RemoteMediaInfoUtil.build(attachment));
        }
        mediaAttachment.setEmbedded(attachment.isEmbedded());
        
        return mediaAttachment;
    }

}
