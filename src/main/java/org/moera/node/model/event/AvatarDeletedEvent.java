package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Avatar;
import org.springframework.data.util.Pair;

public class AvatarDeletedEvent extends Event {

    private String id;
    private String mediaId;

    public AvatarDeletedEvent() {
        super(EventType.AVATAR_DELETED, Scope.VIEW_PROFILE);
    }

    public AvatarDeletedEvent(Avatar avatar) {
        super(EventType.AVATAR_DELETED, Scope.VIEW_PROFILE);
        this.id = avatar.getId().toString();
        this.mediaId = avatar.getMediaFile().getId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(id)));
        parameters.add(Pair.of("mediaId", LogUtil.format(mediaId)));
    }

}
