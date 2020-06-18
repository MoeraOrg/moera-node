package org.moera.node.model.event;

public class RemoteReactionDeletedEvent extends RemoteReactionEvent {

    public RemoteReactionDeletedEvent() {
        super(EventType.REMOTE_REACTION_DELETED);
    }

    public RemoteReactionDeletedEvent(String remoteNodeName, String remotePostingId) {
        super(EventType.REMOTE_REACTION_DELETED, remoteNodeName, remotePostingId);
    }

}
