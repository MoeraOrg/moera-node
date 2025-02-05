package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvatarImage {

    private String mediaId;
    private String path;
    private int width;
    private int height;
    private String shape;

    @JsonIgnore
    private MediaFile mediaFile;

    public AvatarImage() {
    }

    public AvatarImage(Avatar avatar) {
        this(avatar.getMediaFile(), avatar.getShape());
    }

    public AvatarImage(AvatarInfo avatarInfo) {
        mediaId = avatarInfo.getMediaId();
        path = avatarInfo.getPath();
        width = avatarInfo.getWidth();
        height = avatarInfo.getHeight();
        shape = avatarInfo.getShape();
    }

    public AvatarImage(MediaFile mediaFile, String shape) {
        this.mediaFile = mediaFile;
        if (mediaFile != null) {
            mediaId = mediaFile.getId();
            path = "public/" + mediaFile.getFileName();
            width = mediaFile.getSizeX();
            height = mediaFile.getSizeY();
        }
        this.shape = shape;
    }

    public AvatarImage(AvatarDescription avatarDescription, MediaFile mediaFile) {
        this(mediaFile, avatarDescription != null ? avatarDescription.getShape() : null);
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

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public String toLogString() {
        return String.format("AvatarImage(path=%s, shape=%s)", LogUtil.format(path), LogUtil.format(shape));
    }

}
