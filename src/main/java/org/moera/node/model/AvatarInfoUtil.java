package org.moera.node.model;

import org.moera.lib.node.types.AvatarInfo;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.data.Avatar;
import org.moera.node.media.MimeUtils;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.MediaUtil;

public class AvatarInfoUtil {

    public static AvatarInfo build(Avatar avatar, DirectServeConfig config) {
        AvatarInfo avatarInfo = new AvatarInfo();
        avatarInfo.setId(avatar.getId().toString());
        avatarInfo.setMediaId(avatar.getMediaFile().getId());
        avatarInfo.setPath("public/" + avatar.getMediaFile().getFileName());
        avatarInfo.setMimeType(avatar.getMediaFile().getMimeType());
        avatarInfo.setWidth(avatar.getMediaFile().getSizeX());
        avatarInfo.setHeight(avatar.getMediaFile().getSizeY());
        fillDirectPath(avatarInfo, config);
        avatarInfo.setShape(avatar.getShape());
        avatarInfo.setOrdinal(avatar.getOrdinal());
        return avatarInfo;
    }

    private static void fillDirectPath(AvatarInfo info, DirectServeConfig config) {
        var fileName = MimeUtils.fileName(info.getMediaId(), info.getMimeType());
        var pu = MediaUtil.presignDirectPath(fileName, info.getMediaId(), ExtendedDuration.ALWAYS, config);
        info.setDirectPath(pu.url());
        info.setDirectPathExpiresAt(pu.expires());
    }

}
