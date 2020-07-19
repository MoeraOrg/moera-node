package org.moera.node.model.event;

import org.moera.node.data.Comment;

public class CommentDeletedEvent extends CommentEvent {

    public CommentDeletedEvent() {
        super(EventType.COMMENT_DELETED);
    }

    public CommentDeletedEvent(Comment comment) {
        super(EventType.COMMENT_DELETED, comment);
    }

}
