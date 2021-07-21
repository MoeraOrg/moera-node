package org.moera.node.model;

import org.moera.node.data.Avatar;

public class AvatarDescription {

    private String mediaId;
    private String shape;
    private Boolean optional;

    public AvatarDescription() {
    }

    public AvatarDescription(Avatar avatar) {
        if (avatar != null && avatar.getMediaFile() != null) {
            mediaId = avatar.getMediaFile().getId();
            shape = avatar.getShape();
        }
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

}
