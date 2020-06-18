package org.moera.node.model.event;

import org.moera.node.event.EventSubscriber;

public class RemoteReactionEvent extends Event {

    private String remoteNodeName;
    private String remotePostingId;

    protected RemoteReactionEvent(EventType type) {
        super(type);
    }

    protected RemoteReactionEvent(EventType type, String remoteNodeName, String remotePostingId) {
        super(type);
        this.remoteNodeName = remoteNodeName;
        this.remotePostingId = remotePostingId;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
