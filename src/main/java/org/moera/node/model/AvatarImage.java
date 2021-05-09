package org.moera.node.model;

import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;

public class AvatarImage {

    private String mediaId;
    private String path;
    private int width;
    private int height;
    private String shape;

    public AvatarImage() {
    }

    public AvatarImage(Avatar avatar) {
        this(avatar.getMediaFile(), avatar.getShape());
    }

    public AvatarImage(MediaFile mediaFile, String shape) {
        mediaId = mediaFile.getId();
        path = "public/" + mediaFile.getFileName();
        width = mediaFile.getSizeX();
        height = mediaFile.getSizeY();
        this.shape = shape;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
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
