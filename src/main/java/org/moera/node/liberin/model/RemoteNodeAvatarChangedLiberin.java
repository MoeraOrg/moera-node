package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;

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

}
