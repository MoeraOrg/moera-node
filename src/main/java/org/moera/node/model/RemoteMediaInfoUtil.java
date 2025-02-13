package org.moera.node.model;

import org.moera.lib.node.types.RemoteMediaInfo;
import org.moera.node.data.EntryAttachment;
import org.moera.node.util.Util;

public class RemoteMediaInfoUtil {

    public static RemoteMediaInfo build(EntryAttachment attachment) {
        RemoteMediaInfo remoteMediaInfo = new RemoteMediaInfo();
        remoteMediaInfo.setId(attachment.getRemoteMediaId());
        remoteMediaInfo.setHash(attachment.getRemoteMediaHash());
        remoteMediaInfo.setDigest(Util.base64encode(attachment.getRemoteMediaDigest()));
        return remoteMediaInfo;
    }

}