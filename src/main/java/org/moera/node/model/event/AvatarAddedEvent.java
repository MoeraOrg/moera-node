package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.model.AvatarInfo;
import org.springframework.data.util.Pair;

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
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(avatar.getId())));
        parameters.add(Pair.of("mediaId", LogUtil.format(avatar.getMediaId())));
        parameters.add(Pair.of("ordinal", LogUtil.format(avatar.getOrdinal())));
    }

}
