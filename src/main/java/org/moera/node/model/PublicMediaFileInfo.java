package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.MediaFile;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicMediaFileInfo {

    private String id;
    private String path;
    private Integer width;
    private Integer height;
    private short orientation;
    private long size;

    public PublicMediaFileInfo() {
    }

    public PublicMediaFileInfo(MediaFile mediaFile) {
        id = mediaFile.getId();
        path = "public/" + mediaFile.getFileName();
        width = mediaFile.getSizeX();
        height = mediaFile.getSizeY();
        orientation = mediaFile.getOrientation();
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

    public short getOrientation() {
        return orientation;
    }

    public void setOrientation(short orientation) {
        this.orientation = orientation;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
