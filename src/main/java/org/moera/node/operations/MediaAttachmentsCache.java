package org.moera.node.operations;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.moera.lib.node.types.MediaAttachment;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaAttachmentsCache {

    private List<MediaAttachment> attachments;

    public MediaAttachmentsCache() {
    }

    public List<MediaAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MediaAttachment> attachments) {
        this.attachments = attachments;
    }

}
