package org.moera.node.operations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.moera.lib.node.types.MediaAttachment;

public class MediaAttachmentsCache {

    private List<MediaAttachment> owner;
    private Map<String, List<MediaAttachment>> receivers = new HashMap<>();

    public MediaAttachmentsCache() {
    }

    public List<MediaAttachment> getOwner() {
        return owner;
    }

    public void setOwner(List<MediaAttachment> owner) {
        this.owner = owner;
    }

    public Map<String, List<MediaAttachment>> getReceivers() {
        return receivers;
    }

    public void setReceivers(Map<String, List<MediaAttachment>> receivers) {
        this.receivers = receivers;
    }

    public List<MediaAttachment> getCache(String receiverName) {
        return receiverName == null ? owner : receivers.get(receiverName);
    }

    public void putCache(String receiverName, List<MediaAttachment> attachments) {
        if (receiverName == null) {
            owner = attachments;
        } else {
            receivers.put(receiverName, attachments);
        }
    }

}
