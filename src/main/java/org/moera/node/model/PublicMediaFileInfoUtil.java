package org.moera.node.model;

import org.moera.lib.node.types.PublicMediaFileInfo;
import org.moera.node.data.MediaFile;

public class PublicMediaFileInfoUtil {
    
    public static PublicMediaFileInfo build(MediaFile mediaFile) {
        PublicMediaFileInfo info = new PublicMediaFileInfo();
        info.setId(mediaFile.getId());
        info.setPath("public/" + mediaFile.getFileName());
        info.setWidth(mediaFile.getSizeX());
        info.setHeight(mediaFile.getSizeY());
        info.setOrientation(mediaFile.getOrientation());
        info.setSize(mediaFile.getFileSize());
        return info;
    }

}
