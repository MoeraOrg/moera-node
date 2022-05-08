package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Comment;

public class CommentDeletedEvent extends CommentEvent {

    public CommentDeletedEvent() {
        super(EventType.COMMENT_DELETED);
    }

    public CommentDeletedEvent(PrincipalFilter filter) {
        super(EventType.COMMENT_DELETED, filter);
    }

    public CommentDeletedEvent(Comment comment, PrincipalFilter filter) {
        super(EventType.COMMENT_DELETED, comment, filter);
    }

}
