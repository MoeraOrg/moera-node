package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Avatar;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvatarInfo {

    private String id;
    private String mediaId;
    private String path;
    private int width;
    private int height;
    private String shape;
    private int ordinal;

    public AvatarInfo() {
    }

    public AvatarInfo(Avatar avatar) {
        id = avatar.getId().toString();
        mediaId = avatar.getMediaFile().getId();
        path = "public/" + avatar.getMediaFile().getFileName();
        width = avatar.getMediaFile().getSizeX();
        height = avatar.getMediaFile().getSizeY();
        shape = avatar.getShape();
        ordinal = avatar.getOrdinal();
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

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

}
