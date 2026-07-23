package org.moera.node.model;

import org.moera.lib.node.types.MediaLeaseInfo;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.data.MediaLease;
import org.moera.node.media.MediaGrantSupplier;

public class MediaLeaseInfoUtil {

    public static MediaLeaseInfo build(
        MediaLease mediaLease,
        DirectServeOperations directServe,
        MediaGrantSupplier grantSupplier
    ) {
        MediaLeaseInfo mediaLeaseInfo = new MediaLeaseInfo();
        mediaLeaseInfo.setId(mediaLease.getId().toString());
        mediaLeaseInfo.setMedia(
            PrivateMediaFileInfoUtil.build(mediaLease.getMediaFileOwner(), directServe, grantSupplier)
        );
        return mediaLeaseInfo;
    }

}
