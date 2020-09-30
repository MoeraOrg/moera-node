package org.moera.node.model;

import java.util.Set;
import java.util.UUID;

import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriptionType;

public class PostingSubscriptionsInfo {

    private String comments;

    public PostingSubscriptionsInfo() {
    }

    public PostingSubscriptionsInfo(Set<Subscriber> subscribers) {
        comments = findSubscriberId(subscribers, SubscriptionType.POSTING_COMMENTS);
    }

    private static String findSubscriberId(Set<Subscriber> subscribers, SubscriptionType type) {
        return subscribers.stream()
                .filter(sr -> sr.getSubscriptionType() == type)
                .map(Subscriber::getId)
                .map(UUID::toString)
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
