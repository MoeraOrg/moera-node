package org.moera.node.model.event;

public class RemoteCommentUpdatedEvent extends RemoteCommentEvent {

    public RemoteCommentUpdatedEvent() {
        super(EventType.REMOTE_COMMENT_UPDATED);
    }

    public RemoteCommentUpdatedEvent(String remoteNodeName, String remotePostingId, String remoteCommentId) {
        super(EventType.REMOTE_COMMENT_UPDATED, remoteNodeName, remotePostingId, remoteCommentId);
    }

}
