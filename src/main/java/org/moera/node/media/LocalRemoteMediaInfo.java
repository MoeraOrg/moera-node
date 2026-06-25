package org.moera.node.media;

import java.util.Collections;
import java.util.List;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.MediaFilePreviewInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.RemoteMediaInfo;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.option.Options;

public record LocalRemoteMediaInfo(PrivateMediaFileInfo local, RemoteMediaInfo remote) {

    public LocalRemoteMediaInfo(MediaAttachment attachment) {
        this(attachment.getMedia(), attachment.getRemoteMedia());
    }

    public String mediaId() {
        return local != null ? local.getId() : remote != null ? remote.getMediaId() : null;
    }

    public String hash() {
        return local != null ? local.getHash() : remote != null ? remote.getHash() : null;
    }

    public String mimeType() {
        return local != null ? local.getMimeType() : remote != null ? remote.getMimeType() : null;
    }

    public Integer width() {
        return local != null ? local.getWidth() : remote != null ? remote.getWidth() : null;
    }

    public Integer height() {
        return local != null ? local.getHeight() : remote != null ? remote.getHeight() : null;
    }

    public String title() {
        return local != null ? local.getTitle() : remote != null ? remote.getTitle() : null;
    }

    public boolean attachment() {
        return local != null
            ? Boolean.TRUE.equals(local.getAttachment())
            : remote != null && Boolean.TRUE.equals(remote.getAttachment());
    }

    public String textContent() {
        return local != null ? local.getTextContent() : null;
    }

    public List<MediaFilePreviewInfo> previews() {
        return local != null ? local.getPreviews() : Collections.emptyList();
    }

    public String path(NamingCache namingCache, Options options) {
        return path(namingCache, options, false);
    }

    public String path(NamingCache namingCache, Options options, boolean download) {
        if (local != null) {
            String path = "/moera/media/" + (local.getDirectPath() != null ? local.getDirectPath() : local.getPath());
            if (download) {
                if (path.contains("?")) {
                    path += "&download=true";
                } else {
                    path += "?download=true";
                }
            }
            return path;
        }

        if (remote == null || remote.getNodeName() == null || remote.getMediaId() == null) {
            return null;
        }

        String nodeName = remote.getNodeName();
        var nameDetails = namingCache.getFast(nodeName);
        String prefix = nameDetails.getNodeUri() != null
            ? nameDetails.getNodeUri() + "/media/"
            : "/moera/remote-media/" + nodeName + "/";
        String grant = new MediaGrantGenerator(options).generatePublicRemote(remote.getMediaId(), false, null);
        return prefix + MediaUtil.privatePath(remote, null, grant, download);
    }

}
