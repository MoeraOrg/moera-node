package org.moera.node.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class AvatarAttributes {

    @NotBlank
    private String mediaId;

    @Min(value = 0)
    private int clipX;

    @Min(value = 0)
    private int clipY;

    @Min(value = 200)
    private int clipSize;

    @Min(value = 200)
    private int avatarSize;

    private String shape = "circle";

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public int getClipX() {
        return clipX;
    }

    public void setClipX(int clipX) {
        this.clipX = clipX;
    }

    public int getClipY() {
        return clipY;
    }

    public void setClipY(int clipY) {
        this.clipY = clipY;
    }

    public int getClipSize() {
        return clipSize;
    }

    public void setClipSize(int clipSize) {
        this.clipSize = clipSize;
    }

    public int getAvatarSize() {
        return avatarSize;
    }

    public void setAvatarSize(int avatarSize) {
        this.avatarSize = avatarSize;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

}
