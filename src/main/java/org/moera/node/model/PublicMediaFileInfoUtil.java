package org.moera.node.model;

import org.moera.lib.node.types.PublicMediaFileInfo;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.data.MediaFile;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.media.MediaUtil;

public class PublicMediaFileInfoUtil {
    
    public static PublicMediaFileInfo build(MediaFile mediaFile, DirectServeOperations directServe) {
        PublicMediaFileInfo info = new PublicMediaFileInfo();
        info.setId(mediaFile.getId());
        info.setPath(MediaUtil.publicPath(mediaFile));
        info.setMimeType(mediaFile.getMimeType());
        info.setWidth(mediaFile.getSizeX());
        info.setHeight(mediaFile.getSizeY());
        info.setOrientation(mediaFile.getOrientation());
        info.setSize(mediaFile.getFileSize());
        fillDirectPath(info, mediaFile, directServe);
        return info;
    }

    private static void fillDirectPath(
        PublicMediaFileInfo info, MediaFile mediaFile, DirectServeOperations directServe
    ) {
        var pu = directServe.directPath(mediaFile, ExtendedDuration.ALWAYS);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
