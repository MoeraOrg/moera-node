package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Comment;

public class CommentAddedEvent extends CommentEvent {

    public CommentAddedEvent() {
        super(EventType.COMMENT_ADDED);
    }

    public CommentAddedEvent(PrincipalFilter filter) {
        super(EventType.COMMENT_ADDED, filter);
    }

    public CommentAddedEvent(Comment comment, PrincipalFilter filter) {
        super(EventType.COMMENT_ADDED, comment, filter);
    }

}
