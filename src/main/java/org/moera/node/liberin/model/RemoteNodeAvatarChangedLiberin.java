package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.AvatarImage;
import org.moera.node.liberin.Liberin;

public class RemoteNodeAvatarChangedLiberin extends Liberin {

    private String nodeName;
    private AvatarImage avatar;

    public RemoteNodeAvatarChangedLiberin(String nodeName, AvatarImage avatar) {
        this.nodeName = nodeName;
        this.avatar = avatar;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public AvatarImage getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarImage avatar) {
        this.avatar = avatar;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("avatar", avatar);
    }

}
