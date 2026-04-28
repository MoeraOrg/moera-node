package org.moera.node.model;

import org.moera.lib.node.types.PublicMediaFileInfo;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.MediaFile;
import org.moera.node.media.MimeUtils;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.MediaUtil;

public class PublicMediaFileInfoUtil {
    
    public static PublicMediaFileInfo build(MediaFile mediaFile, DirectServeConfig config) {
        PublicMediaFileInfo info = new PublicMediaFileInfo();
        info.setId(mediaFile.getId());
        info.setPath(MediaUtil.publicPath(mediaFile));
        info.setMimeType(mediaFile.getMimeType());
        info.setWidth(mediaFile.getSizeX());
        info.setHeight(mediaFile.getSizeY());
        info.setOrientation(mediaFile.getOrientation());
        info.setSize(mediaFile.getFileSize());
        fillDirectPath(info, config);
        return info;
    }

    private static void fillDirectPath(PublicMediaFileInfo info, DirectServeConfig config) {
        var fileName = MimeUtils.fileName(info.getId(), info.getMimeType());
        var pu = MediaUtil.directPath(fileName, info.getId(), ExtendedDuration.ALWAYS, config);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
