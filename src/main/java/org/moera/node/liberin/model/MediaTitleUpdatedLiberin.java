package org.moera.node.liberin.model;

import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class MediaTitleUpdatedLiberin extends Liberin {

    private UUID mediaId;
    private String title;

    public MediaTitleUpdatedLiberin(UUID mediaId, String title) {
        this.mediaId = mediaId;
        this.title = title;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
