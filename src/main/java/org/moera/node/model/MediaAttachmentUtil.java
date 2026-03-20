package org.moera.node.model;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.EntryAttachment;

public class MediaAttachmentUtil {
    
    public static MediaAttachment build(EntryAttachment attachment, String receiverName, DirectServeConfig config) {
        MediaAttachment mediaAttachment = new MediaAttachment();
        
        if (attachment.getMediaFileOwner() != null) {
            mediaAttachment.setMedia(
                PrivateMediaFileInfoUtil.build(attachment.getMediaFileOwner(), receiverName, config)
            );
        }
        if (attachment.getRemoteMediaId() != null) {
            mediaAttachment.setRemoteMedia(RemoteMediaInfoUtil.build(attachment));
        }
        mediaAttachment.setEmbedded(attachment.isEmbedded());
        
        return mediaAttachment;
    }

}
