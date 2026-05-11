package org.moera.node.model;

import org.moera.lib.node.types.RemoteMediaInfo;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.util.Util;

public class RemoteMediaInfoUtil {

    public static RemoteMediaInfo build(RemoteMediaFile remoteMediaFile) {
        RemoteMediaInfo remoteMediaInfo = new RemoteMediaInfo();
        remoteMediaInfo.setId(remoteMediaFile.getMediaId());
        remoteMediaInfo.setHash(remoteMediaFile.getHash());
        remoteMediaInfo.setDigest(Util.base64encode(remoteMediaFile.getDigest()));
        remoteMediaInfo.setMimeType(remoteMediaFile.getMimeType());
        remoteMediaInfo.setAttachment(remoteMediaFile.isAttachment());
        return remoteMediaInfo;
    }

}
