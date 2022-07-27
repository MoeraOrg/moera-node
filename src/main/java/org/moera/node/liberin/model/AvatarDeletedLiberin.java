package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.Avatar;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarInfo;

public class AvatarDeletedLiberin extends Liberin {

    private Avatar avatar;

    public AvatarDeletedLiberin(Avatar avatar) {
        this.avatar = avatar;
    }

    public Avatar getAvatar() {
        return avatar;
    }

    public void setAvatar(Avatar avatar) {
        this.avatar = avatar;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("avatar", new AvatarInfo(avatar));
    }

}
