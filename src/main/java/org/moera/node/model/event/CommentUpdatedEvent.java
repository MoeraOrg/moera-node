package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Comment;

public class CommentUpdatedEvent extends CommentEvent {

    public CommentUpdatedEvent() {
        super(EventType.COMMENT_UPDATED);
    }

    public CommentUpdatedEvent(PrincipalFilter filter) {
        super(EventType.COMMENT_UPDATED, filter);
    }

    public CommentUpdatedEvent(Comment comment, PrincipalFilter filter) {
        super(EventType.COMMENT_UPDATED, comment, filter);
    }

}
