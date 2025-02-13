package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.Avatar;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarInfoUtil;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("avatar", AvatarInfoUtil.build(avatar));
    }

}
