package org.moera.node.liberin.model;

import org.moera.lib.node.types.AvatarInfo;
import org.moera.node.liberin.Liberin;

public class AvatarAddedLiberin extends Liberin {

    private AvatarInfo avatar;

    public AvatarAddedLiberin(AvatarInfo avatar) {
        this.avatar = avatar;
    }

    public AvatarInfo getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarInfo avatar) {
        this.avatar = avatar;
    }

}
