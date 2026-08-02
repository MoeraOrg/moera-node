package org.moera.node.model;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.Posting;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.media.grant.MediaGrantSupplier;

public class MediaAttachmentUtil {

    public static boolean isVisible(EntryAttachment attachment) {
        return attachment.getRemoteMediaFile() == null || !attachment.getRemoteMediaFile().isInvalid();
    }

    public static MediaAttachment build(
        EntryAttachment attachment,
        DirectServeOperations directServe,
        MediaGrantSupplier grantSupplier
    ) {
        MediaAttachment mediaAttachment = new MediaAttachment();

        Posting mediaPosting = null;
        if (attachment.getMediaFileOwner() != null) {
            mediaAttachment.setMedia(
                PrivateMediaFileInfoUtil.build(attachment.getMediaFileOwner(), directServe, grantSupplier)
            );
            if (attachment.getRemoteMediaFile() != null) {
                mediaAttachment.setRemoteMedia(RemoteMediaInfoUtil.buildMinimal(attachment.getRemoteMediaFile()));
            }
            mediaPosting = attachment.getMediaFileOwner().getPostingByParentMediaEntry(
                attachment.getEntryRevision() != null ? attachment.getEntryRevision().getEntry() : null
            );
        } else if (attachment.getRemoteMediaFile() != null) {
            mediaAttachment.setRemoteMedia(RemoteMediaInfoUtil.build(attachment.getRemoteMediaFile(), grantSupplier));
            mediaPosting = attachment.getRemoteMediaFile().getPostingByParentMediaEntry(
                attachment.getEntryRevision() != null ? attachment.getEntryRevision().getEntry() : null
            );
        }
        mediaAttachment.setPostingId(mediaPosting != null ? mediaPosting.getId().toString() : null);
        mediaAttachment.setEmbedded(attachment.isEmbedded());
        
        return mediaAttachment;
    }

    public static void fillPaths(
        MediaAttachment mediaAttachment,
        MediaGrantSupplier grantSupplier
    ) {
        if (mediaAttachment.getMedia() != null) {
            var media = mediaAttachment.getMedia();
            PrivateMediaFileInfoUtil.fillPath(media, grantSupplier);
            if (media.getPreviews() != null) {
                for (var preview : media.getPreviews()) {
                    MediaFilePreviewInfoUtil.fillPath(preview, media, grantSupplier);
                }
            }
        }
    }

    public static void fillDirectPaths(
        MediaAttachment mediaAttachment,
        DirectServeOperations directServe
    ) {
        if (mediaAttachment.getMedia() != null) {
            var media = mediaAttachment.getMedia();
            PrivateMediaFileInfoUtil.refreshDirectPaths(media, directServe);
            if (media.getPreviews() != null) {
                for (var preview : media.getPreviews()) {
                    MediaFilePreviewInfoUtil.refreshDirectPath(preview, directServe);
                }
            }
        }
    }

    public static void fillRemoteGrants(
        MediaAttachment mediaAttachment,
        MediaGrantSupplier grantSupplier
    ) {
        if (mediaAttachment.getMedia() == null && mediaAttachment.getRemoteMedia() != null) {
            RemoteMediaInfoUtil.fillGrant(mediaAttachment.getRemoteMedia(), grantSupplier);
        }
    }

    public static String mediaId(MediaAttachment attachment) {
        return attachment.getMedia() != null
            ? attachment.getMedia().getId()
            : attachment.getRemoteMedia() != null
              ? attachment.getRemoteMedia().getMediaId()
              : null;
    }

}
