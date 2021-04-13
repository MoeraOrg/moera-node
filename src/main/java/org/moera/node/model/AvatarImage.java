package org.moera.node.model;

import org.moera.node.data.Avatar;

public class AvatarImage {

    private String path;
    private int width;
    private int height;
    private String shape;

    public AvatarImage() {
    }

    public AvatarImage(Avatar avatar) {
        path = "public/" + avatar.getMediaFile().getFileName();
        width = avatar.getMediaFile().getSizeX();
        height = avatar.getMediaFile().getSizeY();
        shape = avatar.getShape();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

}
