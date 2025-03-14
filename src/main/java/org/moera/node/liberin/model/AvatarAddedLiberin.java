package org.moera.node.liberin.model;

import java.util.Map;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("avatar", avatar);
    }

}
