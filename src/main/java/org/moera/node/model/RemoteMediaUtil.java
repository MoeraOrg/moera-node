package org.moera.node.model;

import java.util.UUID;

import org.moera.lib.node.types.RemoteMedia;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.util.Util;

public class RemoteMediaUtil {

    public static void toRemoteMedia(RemoteMedia remoteMedia, MediaFileOwner mediaFileOwner, UUID leaseId) {
        var mediaFile = mediaFileOwner.getMediaFile();
        remoteMedia.setMediaId(mediaFileOwner.getId().toString());
        remoteMedia.setHash(mediaFile.getId());
        remoteMedia.setDigest(Util.base64encode(mediaFile.getDigest()));
        remoteMedia.setMimeType(mediaFile.getMimeType());
        remoteMedia.setWidth(mediaFile.getSizeX());
        remoteMedia.setHeight(mediaFile.getSizeY());
        remoteMedia.setSize(mediaFile.getFileSize());
        remoteMedia.setDuration(mediaFile.getDuration());
        remoteMedia.setTitle(mediaFileOwner.getTitle());
        remoteMedia.setAttachment(!mediaFile.isReasonableImage() && !mediaFile.isVideo());
        remoteMedia.setLeaseId(leaseId.toString());
    }

    public static RemoteMediaFile toNewRemoteMediaFile(UUID nodeId, RemoteMedia remoteMedia) {
        var remoteMediaFile = new RemoteMediaFile();
        remoteMediaFile.setId(UUID.randomUUID());
        remoteMediaFile.setNodeId(nodeId);
        remoteMediaFile.setNodeName(remoteMedia.getNodeName());
        remoteMediaFile.setMediaId(remoteMedia.getMediaId());
        remoteMediaFile.setHash(remoteMedia.getHash());
        remoteMediaFile.setDigest(Util.base64decode(remoteMedia.getDigest()));
        remoteMediaFile.setMimeType(remoteMedia.getMimeType());
        remoteMediaFile.setAttachment(Boolean.TRUE.equals(remoteMedia.getAttachment()));
        remoteMediaFile.setSizeX(remoteMedia.getWidth());
        remoteMediaFile.setSizeY(remoteMedia.getHeight());
        remoteMediaFile.setFileSize(remoteMedia.getSize());
        remoteMediaFile.setDuration(remoteMedia.getDuration());
        remoteMediaFile.setTitle(remoteMedia.getTitle());
        remoteMediaFile.setLeaseId(remoteMedia.getLeaseId());
        return remoteMediaFile;
    }

}
