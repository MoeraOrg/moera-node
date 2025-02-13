package org.moera.node.model.notification;

import java.util.UUID;

import jakarta.validation.Valid;

import org.moera.lib.node.types.ReactionTotalsInfo;

public class PostingReactionsUpdatedNotification extends PostingSubscriberNotification {

    @Valid
    private ReactionTotalsInfo totals;

    public PostingReactionsUpdatedNotification() {
        super(NotificationType.POSTING_REACTIONS_UPDATED);
    }

    public PostingReactionsUpdatedNotification(UUID postingId, ReactionTotalsInfo totals) {
        super(NotificationType.POSTING_REACTIONS_UPDATED, postingId.toString());
        this.totals = totals;
    }

    public ReactionTotalsInfo getTotals() {
        return totals;
    }

    public void setTotals(ReactionTotalsInfo totals) {
        this.totals = totals;
    }

}
