package org.moera.node.liberin.model;

import java.util.List;

import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.node.data.Posting;
import org.moera.node.data.Reaction;
import org.moera.node.liberin.Liberin;

public class PostingReactionTotalsUpdatedLiberin extends Liberin {

    private Posting posting;
    private List<Reaction> addedReactions = List.of();
    private List<Reaction> deletedReactions = List.of();
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

    public List<Reaction> getAddedReactions() {
        return addedReactions;
    }

    public void setAddedReactions(List<Reaction> addedReactions) {
        this.addedReactions = addedReactions;
    }

    public List<Reaction> getDeletedReactions() {
        return deletedReactions;
    }

    public void setDeletedReactions(List<Reaction> deletedReactions) {
        this.deletedReactions = deletedReactions;
    }

}
