package org.moera.node.model;

import org.moera.node.data.MediaFileOwner;

public class MediaFileInfo {

    private String id;
    private long size;

    public MediaFileInfo() {
    }

    public MediaFileInfo(MediaFileOwner mediaFileOwner) {
        id = mediaFileOwner.getId().toString();
        size = mediaFileOwner.getMediaFile().getFileSize();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
