package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaFileInfo {

    private String id;
    private String path;
    private Integer width;
    private Integer height;
    private long size;

    public MediaFileInfo() {
    }

    public MediaFileInfo(MediaFileOwner mediaFileOwner) {
        id = mediaFileOwner.getId().toString();
        path = "private/" + mediaFileOwner.getFileName();
        width = mediaFileOwner.getMediaFile().getSizeX();
        height = mediaFileOwner.getMediaFile().getSizeY();
        size = mediaFileOwner.getMediaFile().getFileSize();
    }

    public MediaFileInfo(MediaFile mediaFile) {
        id = mediaFile.getId();
        path = "public/" + mediaFile.getFileName();
        width = mediaFile.getSizeX();
        height = mediaFile.getSizeY();
        size = mediaFile.getFileSize();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
