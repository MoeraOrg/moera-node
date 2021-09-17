package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Avatar;
import org.springframework.data.util.Pair;

public class AvatarOrderedEvent extends Event {

    private String id;
    private String mediaId;
    private int ordinal;

    public AvatarOrderedEvent() {
        super(EventType.AVATAR_ORDERED);
    }

    public AvatarOrderedEvent(Avatar avatar) {
        super(EventType.AVATAR_ORDERED);
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

    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(id)));
        parameters.add(Pair.of("mediaId", LogUtil.format(mediaId)));
        parameters.add(Pair.of("ordinal", LogUtil.format(ordinal)));
    }

}
