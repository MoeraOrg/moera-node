package org.moera.node.model.event;

import org.moera.lib.node.types.PrivateMediaFileInfo;

public class RemoteMediaDownloadedEvent extends RemoteMediaDownloadEvent {

    private PrivateMediaFileInfo media;

    public RemoteMediaDownloadedEvent() {
        super(EventType.REMOTE_MEDIA_DOWNLOADED);
    }

    public RemoteMediaDownloadedEvent(String nodeName, String mediaId, PrivateMediaFileInfo media) {
        super(EventType.REMOTE_MEDIA_DOWNLOADED, nodeName, mediaId);
        this.media = media;
    }

    public PrivateMediaFileInfo getMedia() {
        return media;
    }

    public void setMedia(PrivateMediaFileInfo media) {
        this.media = media;
    }

}
