package org.moera.node.liberin.model;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionInfoUtil;

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
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        posting = entityManager.merge(posting);
        model.put("posting", new PostingInfo(posting, AccessCheckers.ADMIN));
        if (addedReaction != null) {
            addedReaction = entityManager.merge(addedReaction);
            model.put("addedReaction", ReactionInfoUtil.build(addedReaction, AccessCheckers.ADMIN));
        }
        if (deletedReaction != null) {
            deletedReaction = entityManager.merge(deletedReaction);
            model.put("deletedReaction", ReactionInfoUtil.build(deletedReaction, AccessCheckers.ADMIN));
        }
        model.put("reactionTotals", reactionTotals);
    }

}
