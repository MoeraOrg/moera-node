package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.node.model.ReactionTotalsInfo;

public class PostingReactionsUpdatedNotification extends SubscriberNotification {

    private String postingId;
    private ReactionTotalsInfo totals;

    public PostingReactionsUpdatedNotification() {
        super(NotificationType.POSTING_REACTIONS_UPDATED);
    }

    public PostingReactionsUpdatedNotification(UUID postingId, ReactionTotalsInfo totals) {
        super(NotificationType.POSTING_REACTIONS_UPDATED);
        this.postingId = postingId.toString();
        this.totals = totals;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public ReactionTotalsInfo getTotals() {
        return totals;
    }

    public void setTotals(ReactionTotalsInfo totals) {
        this.totals = totals;
    }

}
