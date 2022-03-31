package org.moera.node.liberin.model;

import org.moera.node.data.Avatar;
import org.moera.node.liberin.Liberin;

public class AvatarOrderedLiberin extends Liberin {

    private Avatar avatar;

    public AvatarOrderedLiberin(Avatar avatar) {
        this.avatar = avatar;
    }

    public Avatar getAvatar() {
        return avatar;
    }

    public void setAvatar(Avatar avatar) {
        this.avatar = avatar;
    }

}
