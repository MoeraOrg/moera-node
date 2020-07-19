package org.moera.node.model.event;

import org.moera.node.data.Comment;

public class CommentUpdatedEvent extends CommentEvent {

    public CommentUpdatedEvent() {
        super(EventType.COMMENT_UPDATED);
    }

    public CommentUpdatedEvent(Comment comment) {
        super(EventType.COMMENT_UPDATED, comment);
    }

}
