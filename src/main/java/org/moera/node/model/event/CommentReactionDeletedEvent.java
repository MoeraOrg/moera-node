package org.moera.node.model.event;

import org.moera.lib.node.types.ReactionInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Comment;

public class CommentReactionDeletedEvent extends CommentEvent {

    private ReactionInfo reaction;

    public CommentReactionDeletedEvent() {
        super(EventType.COMMENT_REACTION_DELETED);
    }

    public CommentReactionDeletedEvent(Comment comment, ReactionInfo reaction, PrincipalFilter filter) {
        super(EventType.COMMENT_REACTION_DELETED, comment, filter);
        this.reaction = reaction;
    }

    public ReactionInfo getReaction() {
        return reaction;
    }

    public void setReaction(ReactionInfo reaction) {
        this.reaction = reaction;
    }

}
