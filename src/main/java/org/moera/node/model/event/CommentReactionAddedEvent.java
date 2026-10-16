package org.moera.node.model.event;

import org.moera.lib.node.types.ReactionInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Comment;

public class CommentReactionAddedEvent extends CommentEvent {

    private ReactionInfo reaction;

    public CommentReactionAddedEvent() {
        super(EventType.COMMENT_REACTION_ADDED);
    }

    public CommentReactionAddedEvent(Comment comment, ReactionInfo reaction, PrincipalFilter filter) {
        super(EventType.COMMENT_REACTION_ADDED, comment, filter);
        this.reaction = reaction;
    }

    public ReactionInfo getReaction() {
        return reaction;
    }

    public void setReaction(ReactionInfo reaction) {
        this.reaction = reaction;
    }

}
