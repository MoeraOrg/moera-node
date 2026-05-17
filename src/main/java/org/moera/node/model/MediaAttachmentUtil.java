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
            mediaAttachment.setMedia(
                PrivateMediaFileInfoUtil.build(attachment.getMediaFileOwner(), config, grantSupplier)
            );
            if (attachment.getRemoteMediaFile() != null) {
                mediaAttachment.setRemoteMedia(RemoteMediaInfoUtil.buildMinimal(attachment.getRemoteMediaFile()));
            }
            Posting mediaPosting = attachment.getMediaFileOwner().getPostingByParentMediaEntry(
                attachment.getEntryRevision() != null ? attachment.getEntryRevision().getEntry() : null
            );
            mediaAttachment.setPostingId(mediaPosting != null ? mediaPosting.getId().toString() : null);
        } else if (attachment.getRemoteMediaFile() != null) {
            mediaAttachment.setRemoteMedia(RemoteMediaInfoUtil.build(attachment.getRemoteMediaFile(), grantSupplier));
        }
        mediaAttachment.setEmbedded(attachment.isEmbedded());
        
        return mediaAttachment;
    }

    public static void fillPaths(
        MediaAttachment mediaAttachment,
        DirectServeConfig config,
        MediaGrantSupplier grantSupplier
    ) {
        if (mediaAttachment.getMedia() != null) {
            var media = mediaAttachment.getMedia();
            PrivateMediaFileInfoUtil.fillPath(media, grantSupplier);
            PrivateMediaFileInfoUtil.fillDirectPath(media, config);
            if (media.getPreviews() != null) {
                for (var preview : media.getPreviews()) {
                    MediaFilePreviewInfoUtil.fillPath(preview, media, grantSupplier);
                    MediaFilePreviewInfoUtil.fillDirectPath(preview, config);
                }
            }
        } else if (mediaAttachment.getRemoteMedia() != null) {
            RemoteMediaInfoUtil.fillGrant(mediaAttachment.getRemoteMedia(), grantSupplier);
        }
    }

}
