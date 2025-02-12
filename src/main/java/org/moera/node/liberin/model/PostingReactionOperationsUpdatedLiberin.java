package org.moera.node.liberin.model;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionInfo;

public class PostingReactionOperationsUpdatedLiberin extends Liberin {

    private Posting posting;
    private Reaction reaction;

    public PostingReactionOperationsUpdatedLiberin(Posting posting, Reaction reaction) {
        this.posting = posting;
        this.reaction = reaction;
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

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        posting = entityManager.merge(posting);
        reaction = entityManager.merge(reaction);
        model.put("posting", new PostingInfo(posting, AccessCheckers.ADMIN));
        model.put("reaction", new ReactionInfo(reaction, AccessCheckers.ADMIN));
    }

}
