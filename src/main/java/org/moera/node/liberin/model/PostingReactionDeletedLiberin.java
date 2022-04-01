package org.moera.node.liberin.model;

import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.ReactionTotalsInfo;

public class PostingReactionDeletedLiberin extends Liberin {

    private Posting posting;
    private Reaction reaction;
    private ReactionTotalsInfo reactionTotals;

    public PostingReactionDeletedLiberin(Posting posting) {
        this.posting = posting;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public void setReaction(Reaction reaction) {
        this.reaction = reaction;
    }

    public ReactionTotalsInfo getReactionTotals() {
        return reactionTotals;
    }

    public void setReactionTotals(ReactionTotalsInfo reactionTotals) {
        this.reactionTotals = reactionTotals;
    }

}
