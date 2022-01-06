package org.moera.node.model.event;

public class RemotePostingDeletedEvent extends RemotePostingEvent {

    public RemotePostingDeletedEvent() {
        super(EventType.REMOTE_POSTING_DELETED);
    }

    public RemotePostingDeletedEvent(String remoteNodeName, String remotePostingId) {
        super(EventType.REMOTE_POSTING_DELETED, remoteNodeName, remotePostingId);
    }

}
