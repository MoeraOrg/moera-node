package org.moera.node.model.event;

import org.moera.node.data.Comment;

public class CommentReactionsChangedEvent extends CommentEvent {

    public CommentReactionsChangedEvent() {
        super(EventType.COMMENT_REACTIONS_CHANGED);
    }

    public CommentReactionsChangedEvent(Comment comment) {
        super(EventType.COMMENT_REACTIONS_CHANGED, comment);
    }

}
