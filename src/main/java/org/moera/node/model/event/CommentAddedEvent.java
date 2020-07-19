package org.moera.node.model.event;

import org.moera.node.data.Comment;

public class CommentAddedEvent extends CommentEvent {

    public CommentAddedEvent() {
        super(EventType.COMMENT_ADDED);
    }

    public CommentAddedEvent(Comment comment) {
        super(EventType.COMMENT_ADDED, comment);
    }

}
