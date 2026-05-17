package org.moera.node.model;

import java.time.Duration;

import org.moera.lib.node.types.RemoteMediaInfo;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.media.MediaGrantSupplier;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Util;

public class RemoteMediaInfoUtil {

    public static RemoteMediaInfo buildMinimal(RemoteMediaFile remoteMediaFile) {
        RemoteMediaInfo info = new RemoteMediaInfo();
        info.setId(remoteMediaFile.getId().toString());
        info.setNodeName(remoteMediaFile.getNodeName());
        info.setMediaId(remoteMediaFile.getMediaId());
        return info;
    }

    public static RemoteMediaInfo build(RemoteMediaFile remoteMediaFile, MediaGrantSupplier grantSupplier) {
        RemoteMediaInfo info = buildMinimal(remoteMediaFile);
        info.setHash(remoteMediaFile.getHash());
        info.setDigest(Util.base64encode(remoteMediaFile.getDigest()));
        info.setMimeType(remoteMediaFile.getMimeType());
        info.setAttachment(remoteMediaFile.isAttachment());
        info.setWidth(remoteMediaFile.getSizeX());
        info.setHeight(remoteMediaFile.getSizeY());
        info.setSize(remoteMediaFile.getFileSize());
        fillGrant(info, grantSupplier);
        return info;
    }

    public static void fillGrant(RemoteMediaInfo info, MediaGrantSupplier grantSupplier) {
        ExtendedDuration valid = new ExtendedDuration(Duration.ofDays(3));
        String grant = grantSupplier != null
            ? grantSupplier.generateRemote(info.getMediaId(), valid, false, null)
            : null;
        info.setGrant(grant);
    }

}
