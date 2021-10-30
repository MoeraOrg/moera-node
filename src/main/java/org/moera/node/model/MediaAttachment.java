package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.EntryAttachment;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaAttachment {

    private PrivateMediaFileInfo media;
    private boolean embedded;

    public MediaAttachment() {
    }

    public MediaAttachment(EntryAttachment attachment) {
        media = new PrivateMediaFileInfo(attachment.getMediaFileOwner());
        embedded = attachment.isEmbedded();
    }

    public PrivateMediaFileInfo getMedia() {
        return media;
    }

    public void setMedia(PrivateMediaFileInfo media) {
        this.media = media;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

}
