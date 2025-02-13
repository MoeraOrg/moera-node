package org.moera.node.model;

import org.moera.lib.node.types.AvatarInfo;
import org.moera.node.data.Avatar;

public class AvatarInfoUtil {

    public static AvatarInfo build(Avatar avatar) {
        AvatarInfo avatarInfo = new AvatarInfo();
        avatarInfo.setId(avatar.getId().toString());
        avatarInfo.setMediaId(avatar.getMediaFile().getId());
        avatarInfo.setPath("public/" + avatar.getMediaFile().getFileName());
        avatarInfo.setWidth(avatar.getMediaFile().getSizeX());
        avatarInfo.setHeight(avatar.getMediaFile().getSizeY());
        avatarInfo.setShape(avatar.getShape());
        avatarInfo.setOrdinal(avatar.getOrdinal());
        return avatarInfo;
    }

}
