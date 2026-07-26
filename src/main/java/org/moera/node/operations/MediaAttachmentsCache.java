package org.moera.node.operations;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.config.DirectServeSource;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaAttachmentsCache {

    private List<MediaAttachment> attachments;
    private DirectServeSource directServeSource;

    public MediaAttachmentsCache() {
    }

    public List<MediaAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MediaAttachment> attachments) {
        this.attachments = attachments;
    }

    public DirectServeSource getDirectServeSource() {
        return directServeSource;
    }

    public void setDirectServeSource(DirectServeSource directServeSource) {
        this.directServeSource = directServeSource;
    }

}
