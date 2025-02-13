package org.moera.node.model;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.data.EntryAttachment;

public class MediaAttachmentUtil {
    
    public static MediaAttachment build(EntryAttachment attachment, String receiverName) {
        MediaAttachment mediaAttachment = new MediaAttachment();
        
        if (attachment.getMediaFileOwner() != null) {
            mediaAttachment.setMedia(PrivateMediaFileInfoUtil.build(attachment.getMediaFileOwner(), receiverName));
        }
        if (attachment.getRemoteMediaId() != null) {
            mediaAttachment.setRemoteMedia(RemoteMediaInfoUtil.build(attachment));
        }
        mediaAttachment.setEmbedded(attachment.isEmbedded());
        
        return mediaAttachment;
    }

}
