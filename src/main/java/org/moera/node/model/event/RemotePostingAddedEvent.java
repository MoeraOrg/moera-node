package org.moera.node.model.event;

public class RemotePostingAddedEvent extends RemotePostingEvent {

    public RemotePostingAddedEvent() {
        super(EventType.REMOTE_POSTING_ADDED);
    }

    public RemotePostingAddedEvent(String remoteNodeName, String remotePostingId) {
        super(EventType.REMOTE_POSTING_ADDED, remoteNodeName, remotePostingId);
    }

}
