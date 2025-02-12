package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Comment;

public class CommentReactionsChangedEvent extends CommentEvent {

    public CommentReactionsChangedEvent() {
        super(EventType.COMMENT_REACTIONS_CHANGED);
    }

    public CommentReactionsChangedEvent(PrincipalFilter filter) {
        super(EventType.COMMENT_REACTIONS_CHANGED, filter);
    }

    public CommentReactionsChangedEvent(Comment comment, PrincipalFilter filter) {
        super(EventType.COMMENT_REACTIONS_CHANGED, comment, filter);
    }

}
