package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import org.moera.lib.node.types.MediaAttachment;

public class MediaAttachmentsCache {

    private MediaAttachment[] owner;
    private Map<String, MediaAttachment[]> receivers = new HashMap<>();

    public MediaAttachmentsCache() {
    }

    public MediaAttachment[] getOwner() {
        return owner;
    }

    public void setOwner(MediaAttachment[] owner) {
        this.owner = owner;
    }

    public Map<String, MediaAttachment[]> getReceivers() {
        return receivers;
    }

    public void setReceivers(Map<String, MediaAttachment[]> receivers) {
        this.receivers = receivers;
    }

    public MediaAttachment[] getCache(String receiverName) {
        return receiverName == null ? owner : receivers.get(receiverName);
    }

    public void putCache(String receiverName, MediaAttachment[] attachments) {
        if (receiverName == null) {
            owner = attachments;
        } else {
            receivers.put(receiverName, attachments);
        }
    }

}
