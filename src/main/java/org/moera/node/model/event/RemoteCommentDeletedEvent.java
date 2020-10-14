package org.moera.node.model.event;

public class RemoteCommentDeletedEvent extends RemoteCommentEvent {

    public RemoteCommentDeletedEvent() {
        super(EventType.REMOTE_COMMENT_DELETED);
    }

    public RemoteCommentDeletedEvent(String remoteNodeName, String remotePostingId, String remoteCommentId) {
        super(EventType.REMOTE_COMMENT_DELETED, remoteNodeName, remotePostingId, remoteCommentId);
    }

}
