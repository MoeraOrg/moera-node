package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.ReactionTotalsInfo;

public class PostingReactionAddedLiberin extends Liberin {

    private Posting posting;
    private Reaction addedReaction;
    private Reaction deletedReaction;
    private ReactionTotalsInfo reactionTotals;

    public PostingReactionAddedLiberin(Posting posting) {
        this.posting = posting;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public Reaction getAddedReaction() {
        return addedReaction;
    }

    public void setAddedReaction(Reaction addedReaction) {
        this.addedReaction = addedReaction;
    }

    public Reaction getDeletedReaction() {
        return deletedReaction;
    }

    public void setDeletedReaction(Reaction deletedReaction) {
        this.deletedReaction = deletedReaction;
    }

    public ReactionTotalsInfo getReactionTotals() {
        return reactionTotals;
    }

    public void setReactionTotals(ReactionTotalsInfo reactionTotals) {
        this.reactionTotals = reactionTotals;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("posting", new PostingInfo(posting, AccessCheckers.ADMIN));
        if (addedReaction != null) {
            model.put("addedReaction", new ReactionInfo(addedReaction, AccessCheckers.ADMIN));
        }
        if (deletedReaction != null) {
            model.put("deletedReaction", new ReactionInfo(deletedReaction, AccessCheckers.ADMIN));
        }
        model.put("reactionTotals", reactionTotals);
    }

}
