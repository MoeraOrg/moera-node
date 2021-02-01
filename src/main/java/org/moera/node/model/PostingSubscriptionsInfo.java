package org.moera.node.model;

import java.util.Collection;
import java.util.UUID;

import org.moera.node.data.Subscriber;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionType;

public class PostingSubscriptionsInfo {

    private String comments;

    public PostingSubscriptionsInfo() {
    }

    public static PostingSubscriptionsInfo fromSubscribers(Collection<Subscriber> subscribers) {
        var info = new PostingSubscriptionsInfo();
        info.setComments(findIdInSubscribers(subscribers, SubscriptionType.POSTING_COMMENTS));
        return info;
    }

    public static PostingSubscriptionsInfo fromSubscriptions(Collection<Subscription> subscriptions) {
        var info = new PostingSubscriptionsInfo();
        info.setComments(findIdInSubscriptions(subscriptions, SubscriptionType.POSTING_COMMENTS));
        return info;
    }

    private static String findIdInSubscribers(Collection<Subscriber> subscribers, SubscriptionType type) {
        if (subscribers == null || subscribers.isEmpty()) {
            return null;
        }
        return subscribers.stream()
                .filter(sr -> sr.getSubscriptionType() == type)
                .map(Subscriber::getId)
                .map(UUID::toString)
                .findFirst()
                .orElse(null);
    }

    private static String findIdInSubscriptions(Collection<Subscription> subscriptions, SubscriptionType type) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return null;
        }
        return subscriptions.stream()
                .filter(sr -> sr.getSubscriptionType() == type)
                .map(Subscription::getRemoteSubscriberId)
                .findFirst()
                .orElse(null);
    }

    public void setSubscriberId(SubscriptionType type, String id) {
        switch (type) {
            case POSTING_COMMENTS:
                setComments(id);
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Subscription type %s is not allowed here", type.name()));
        }
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

}
