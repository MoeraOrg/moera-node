package org.moera.node.model;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionTotal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReactionCreated {

    private ReactionInfo reaction;
    private ReactionTotalsInfo totals;

    public ReactionCreated() {
    }

    public ReactionCreated(Reaction reaction, Collection<ReactionTotal> totals) {
        this.reaction = new ReactionInfo(reaction);
        this.totals = new ReactionTotalsInfo(totals);
    }

    public ReactionInfo getReaction() {
        return reaction;
    }

    public void setReaction(ReactionInfo reaction) {
        this.reaction = reaction;
    }

    public ReactionTotalsInfo getTotals() {
        return totals;
    }

    public void setTotals(ReactionTotalsInfo totals) {
        this.totals = totals;
    }

}
