package org.moera.node.liberin.model;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.ReactionTotalsInfo;

public class PostingReactionTotalsUpdatedLiberin extends Liberin {

    private Posting posting;
    private ReactionTotalsInfo totals;

    public PostingReactionTotalsUpdatedLiberin(Posting posting, ReactionTotalsInfo totals) {
        this.posting = posting;
        this.totals = totals;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public ReactionTotalsInfo getTotals() {
        return totals;
    }

    public void setTotals(ReactionTotalsInfo totals) {
        this.totals = totals;
    }

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        posting = entityManager.merge(posting);
        model.put("posting", new PostingInfo(posting, AccessCheckers.ADMIN));
        model.put("totals", totals);
    }

}
