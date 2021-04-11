package org.moera.node.model;

import org.moera.node.data.Avatar;

public class AvatarInfo {

    private String id;
    private String mediaId;
    private int width;
    private int height;
    private boolean current;
    private String shape;

    public AvatarInfo() {
    }

    public AvatarInfo(Avatar avatar) {
        id = avatar.getId().toString();
        mediaId = avatar.getMediaFile().getId();
        width = avatar.getMediaFile().getSizeX();
        height = avatar.getMediaFile().getSizeY();
        current = avatar.isCurrent();
        shape = avatar.getShape();
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

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

}
