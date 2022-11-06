package org.moera.node.model;

import java.util.Collection;
import java.util.UUID;

import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;

public class PostingSubscriptionsInfo {

    private UUID comments;

    public PostingSubscriptionsInfo() {
    }

    public PostingSubscriptionsInfo(Collection<UserSubscription> subscriptions) {
        comments = findIdInSubscriptions(subscriptions, SubscriptionType.POSTING_COMMENTS);
    }

    private static UUID findIdInSubscriptions(Collection<UserSubscription> subscriptions, SubscriptionType type) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return null;
        }
        return subscriptions.stream()
                .filter(sr -> sr.getSubscriptionType() == type)
                .map(UserSubscription::getId)
                .findFirst()
                .orElse(null);
    }

    public UUID getComments() {
        return comments;
    }

    public void setComments(UUID comments) {
        this.comments = comments;
    }

}
