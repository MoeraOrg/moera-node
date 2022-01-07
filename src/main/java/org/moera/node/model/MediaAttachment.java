package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.EntryAttachment;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaAttachment {

    private PrivateMediaFileInfo media;
    private RemoteMediaInfo remoteMedia;
    private boolean embedded;

    public MediaAttachment() {
    }

    public MediaAttachment(EntryAttachment attachment, String receiverName) {
        if (attachment.getMediaFileOwner() != null) {
            media = new PrivateMediaFileInfo(attachment.getMediaFileOwner(), receiverName);
        }
        if (attachment.getRemoteMediaId() != null) {
            remoteMedia = new RemoteMediaInfo(attachment);
        }
        embedded = attachment.isEmbedded();
    }

    public PrivateMediaFileInfo getMedia() {
        return media;
    }

    public void setMedia(PrivateMediaFileInfo media) {
        this.media = media;
    }

    public RemoteMediaInfo getRemoteMedia() {
        return remoteMedia;
    }

    public void setRemoteMedia(RemoteMediaInfo remoteMedia) {
        this.remoteMedia = remoteMedia;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

}
