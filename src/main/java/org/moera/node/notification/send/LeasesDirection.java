package org.moera.node.notification.send;

import java.util.UUID;

class LeasesDirection extends Direction {

    private UUID mediaId;

    LeasesDirection(UUID nodeId, UUID mediaId) {
        super(nodeId);
        this.mediaId = mediaId;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }

}
