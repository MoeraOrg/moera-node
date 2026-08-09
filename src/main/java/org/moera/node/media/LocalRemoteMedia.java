package org.moera.node.media;

import org.moera.node.data.Entry;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaLease;
import org.moera.node.data.Posting;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.global.ServeContext;
import org.moera.node.media.grant.MediaGrantGenerator;

public record LocalRemoteMedia(
    MediaFileOwner mediaFileOwner,
    RemoteMediaFile remoteMediaFile,
    MediaLease mediaLease
) {

    public LocalRemoteMedia(MediaFileOwner mediaFileOwner, RemoteMediaFile remoteMediaFile) {
        this(mediaFileOwner, remoteMediaFile, null);
    }

    public String mediaId() {
        return mediaFileOwner != null
            ? mediaFileOwner.getId().toString()
            : remoteMediaFile != null
              ? remoteMediaFile.getMediaId()
              : null;
    }

    public String hash() {
        return mediaFileOwner != null
            ? mediaFileOwner.getMediaFile().getId()
            : remoteMediaFile != null
              ? remoteMediaFile.getHash()
              : null;
    }

    public byte[] digest() {
        return mediaFileOwner != null
            ? mediaFileOwner.getMediaFile().getDigest()
            : remoteMediaFile != null
              ? remoteMediaFile.getDigest()
              : null;
    }

    public String mimeType() {
        return mediaFileOwner != null
            ? mediaFileOwner.getMediaFile().getMimeType()
            : remoteMediaFile != null
            ? remoteMediaFile.getMimeType()
            : null;
    }

    public boolean attachment() {
        return mediaFileOwner != null
            ? !mediaFileOwner.getMediaFile().isImage()
            : remoteMediaFile != null && remoteMediaFile.isAttachment();
    }

    public String path(ServeContext context, Integer width) {
        if (mediaFileOwner != null) {
            String directPath = context.directServeOperations().directPath(mediaFileOwner).url();
            return MediaUtil.mediaUrl(
                directPath != null ? directPath : MediaUtil.privatePath(mediaFileOwner, width, null)
            );
        }

        if (remoteMediaFile == null || remoteMediaFile.getNodeName() == null || remoteMediaFile.getMediaId() == null) {
            return null;
        }

        String grant = new MediaGrantGenerator(context.options())
            .generatePublicRemote(remoteMediaFile.getMediaId(), false, null);
        return "/moera/remote-media/" + remoteMediaFile.getNodeName() + "/"
            + MediaUtil.privatePath(remoteMediaFile, width, grant);
    }

    public Posting postingByParentMediaEntry(Entry parentMediaEntry) {
        return mediaFileOwner != null
            ? mediaFileOwner.getPostingByParentMediaEntry(parentMediaEntry)
            : remoteMediaFile != null
              ? remoteMediaFile.getPostingByParentMediaEntry(parentMediaEntry)
              : null;
    }

}
