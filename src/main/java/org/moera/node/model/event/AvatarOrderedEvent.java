package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Avatar;

public class AvatarOrderedEvent extends Event {

    private String id;
    private String mediaId;
    private int ordinal;

    public AvatarOrderedEvent() {
        super(EventType.AVATAR_ORDERED, Scope.VIEW_PROFILE);
    }

    public AvatarOrderedEvent(Avatar avatar) {
        super(EventType.AVATAR_ORDERED, Scope.VIEW_PROFILE);
        this.id = avatar.getId().toString();
        this.mediaId = avatar.getMediaFile().getId();
        this.ordinal = avatar.getOrdinal();
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

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("id", LogUtil.format(id)));
        parameters.add(new LogParameter("mediaId", LogUtil.format(mediaId)));
        parameters.add(new LogParameter("ordinal", LogUtil.format(ordinal)));
    }

}
