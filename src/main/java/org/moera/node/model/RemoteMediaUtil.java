package org.moera.node.model;

import java.util.UUID;

import org.moera.lib.node.types.RemoteMedia;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.util.Util;

public class RemoteMediaUtil {

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
        remoteMediaFile.setTitle(remoteMedia.getTitle());
        remoteMediaFile.setLeaseId(remoteMedia.getLeaseId());
        return remoteMediaFile;
    }

}
