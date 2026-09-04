package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.model.AvatarImageUtil;

public class RemoteNodeAvatarChangedEvent extends Event {

    private String name;
    private AvatarImage avatar;

    public RemoteNodeAvatarChangedEvent() {
        super(EventType.REMOTE_NODE_AVATAR_CHANGED, Scope.VIEW_PEOPLE);
    }

    public RemoteNodeAvatarChangedEvent(String name, AvatarImage avatar) {
        this();
        this.name = name;
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AvatarImage getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarImage avatar) {
        this.avatar = avatar;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("name", LogUtil.format(name)));
        parameters.add(new LogParameter("avatar", avatar != null ? AvatarImageUtil.toLogString(avatar) : "null"));
    }

}
