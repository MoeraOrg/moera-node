package org.moera.node.model;

import org.moera.lib.node.types.AvatarInfo;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.media.MediaUtil;

public class AvatarInfoUtil {

    public static AvatarInfo build(Avatar avatar, DirectServeConfig config) {
        AvatarInfo avatarInfo = new AvatarInfo();
        avatarInfo.setId(avatar.getId().toString());
        avatarInfo.setMediaId(avatar.getMediaFile().getId());
        avatarInfo.setPath(MediaUtil.publicPath(avatar.getMediaFile()));
        avatarInfo.setMimeType(avatar.getMediaFile().getMimeType());
        avatarInfo.setWidth(avatar.getMediaFile().getSizeX());
        avatarInfo.setHeight(avatar.getMediaFile().getSizeY());
        fillDirectPath(avatarInfo, avatar.getMediaFile(), config);
        avatarInfo.setShape(avatar.getShape());
        avatarInfo.setOrdinal(avatar.getOrdinal());
        return avatarInfo;
    }

    private static void fillDirectPath(
        AvatarInfo info, MediaFile mediaFile, DirectServeConfig config
    ) {
        var pu = MediaUtil.directPath(mediaFile, ExtendedDuration.ALWAYS, config);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
