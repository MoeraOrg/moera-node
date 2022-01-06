package org.moera.node.model.event;

public class RemotePostingUpdatedEvent extends RemotePostingEvent {

    public RemotePostingUpdatedEvent() {
        super(EventType.REMOTE_POSTING_UPDATED);
    }

    public RemotePostingUpdatedEvent(String remoteNodeName, String remotePostingId) {
        super(EventType.REMOTE_POSTING_UPDATED, remoteNodeName, remotePostingId);
    }

}
