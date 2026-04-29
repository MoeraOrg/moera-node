package org.moera.node.model;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.EntryAttachment;
import org.moera.node.media.MediaGrantSupplier;

public class MediaAttachmentUtil {

    public static MediaAttachment build(
        EntryAttachment attachment,
        String receiverName,
        DirectServeConfig config,
        MediaGrantSupplier grantSupplier
    ) {
        MediaAttachment mediaAttachment = new MediaAttachment();
        
        if (attachment.getMediaFileOwner() != null) {
            mediaAttachment.setMedia(
                PrivateMediaFileInfoUtil.build(attachment.getMediaFileOwner(), receiverName, config, grantSupplier)
            );
        }
        if (attachment.getRemoteMediaId() != null) {
            mediaAttachment.setRemoteMedia(RemoteMediaInfoUtil.build(attachment));
        }
        mediaAttachment.setEmbedded(attachment.isEmbedded());
        
        return mediaAttachment;
    }

}
