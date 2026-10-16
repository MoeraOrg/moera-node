package org.moera.node.model.event;

import org.moera.lib.node.types.ReactionInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Posting;

public class PostingReactionDeletedEvent extends PostingEvent {

    private ReactionInfo reaction;

    public PostingReactionDeletedEvent() {
        super(EventType.POSTING_REACTION_DELETED);
    }

    public PostingReactionDeletedEvent(Posting posting, ReactionInfo reaction, PrincipalFilter filter) {
        super(EventType.POSTING_REACTION_DELETED, posting, filter);
        this.reaction = reaction;
    }

    public ReactionInfo getReaction() {
        return reaction;
    }

    public void setReaction(ReactionInfo reaction) {
        this.reaction = reaction;
    }

}
