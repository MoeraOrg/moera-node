package org.moera.node.model.event;

public class RemoteCommentAddedEvent extends RemoteCommentEvent {

    public RemoteCommentAddedEvent() {
        super(EventType.REMOTE_COMMENT_ADDED);
    }

    public RemoteCommentAddedEvent(String remoteNodeName, String remotePostingId, String remoteCommentId) {
        super(EventType.REMOTE_COMMENT_ADDED, remoteNodeName, remotePostingId, remoteCommentId);
    }

}
