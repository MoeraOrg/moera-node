package org.moera.node.model;

import org.moera.lib.node.types.AvatarInfo;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.media.MediaUtil;

public class AvatarInfoUtil {

    public static AvatarInfo build(Avatar avatar, DirectServeOperations directServe) {
        AvatarInfo avatarInfo = new AvatarInfo();
        avatarInfo.setId(avatar.getId().toString());
        avatarInfo.setMediaId(avatar.getMediaFile().getId());
        avatarInfo.setPath(MediaUtil.publicPath(avatar.getMediaFile()));
        avatarInfo.setMimeType(avatar.getMediaFile().getMimeType());
        avatarInfo.setWidth(avatar.getMediaFile().getSizeX());
        avatarInfo.setHeight(avatar.getMediaFile().getSizeY());
        fillDirectPath(avatarInfo, avatar.getMediaFile(), directServe);
        avatarInfo.setShape(avatar.getShape());
        avatarInfo.setOrdinal(avatar.getOrdinal());
        return avatarInfo;
    }

    private static void fillDirectPath(
        AvatarInfo info, MediaFile mediaFile, DirectServeOperations directServe
    ) {
        var pu = directServe.directPath(mediaFile, ExtendedDuration.ALWAYS);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
