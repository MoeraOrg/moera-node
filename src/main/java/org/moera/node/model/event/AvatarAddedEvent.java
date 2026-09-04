package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.AvatarInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;

public class AvatarAddedEvent extends Event {

    private AvatarInfo avatar;

    public AvatarAddedEvent() {
        super(EventType.AVATAR_ADDED, Scope.VIEW_PROFILE);
    }

    public AvatarAddedEvent(AvatarInfo avatar) {
        super(EventType.AVATAR_ADDED, Scope.VIEW_PROFILE);
        this.avatar = avatar;
    }

    public AvatarInfo getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarInfo avatar) {
        this.avatar = avatar;
    }

    @Override
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("id", LogUtil.format(avatar.getId())));
        parameters.add(new LogParameter("mediaId", LogUtil.format(avatar.getMediaId())));
        parameters.add(new LogParameter("ordinal", LogUtil.format(avatar.getOrdinal())));
    }

}
