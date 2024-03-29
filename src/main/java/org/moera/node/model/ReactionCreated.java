package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.data.Reaction;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReactionCreated {

    private ReactionInfo reaction;
    private ReactionTotalsInfo totals;

    public ReactionCreated() {
    }

    public ReactionCreated(Reaction reaction, ReactionTotalsInfo totals, AccessChecker accessChecker) {
        this.reaction = reaction != null ? new ReactionInfo(reaction, accessChecker) : null;
        this.totals = totals;
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
