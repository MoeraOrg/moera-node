package org.moera.node.media;

import org.moera.node.data.Entry;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaLease;
import org.moera.node.data.Posting;
import org.moera.node.data.RemoteMediaFile;

public record LocalRemoteMedia(
    MediaFileOwner mediaFileOwner,
    RemoteMediaFile remoteMediaFile,
    MediaLease mediaLease
) {

    public LocalRemoteMedia(MediaFileOwner mediaFileOwner, RemoteMediaFile remoteMediaFile) {
        this(mediaFileOwner, remoteMediaFile, null);
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

    public boolean attachment() {
        return mediaFileOwner != null
            ? !mediaFileOwner.getMediaFile().isImage()
            : remoteMediaFile != null && remoteMediaFile.isAttachment();
    }

    public Posting postingByParentMediaEntry(Entry parentMediaEntry) {
        return mediaFileOwner != null
            ? mediaFileOwner.getPostingByParentMediaEntry(parentMediaEntry)
            : remoteMediaFile != null
              ? remoteMediaFile.getPostingByParentMediaEntry(parentMediaEntry)
              : null;
    }

}
