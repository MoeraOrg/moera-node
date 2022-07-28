package org.moera.node.liberin.model;

import java.util.Map;

import javax.persistence.EntityManager;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionInfo;
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

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        posting = entityManager.merge(posting);
        reaction = entityManager.merge(reaction);
        model.put("posting", new PostingInfo(posting, AccessCheckers.ADMIN));
        model.put("reaction", new ReactionInfo(reaction, AccessCheckers.ADMIN));
        model.put("reactionTotals", reactionTotals);
    }

}
