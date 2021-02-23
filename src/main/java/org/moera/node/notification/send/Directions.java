package org.moera.node.notification.send;

import java.util.UUID;

import org.moera.node.data.SubscriptionType;

public class Directions {

    public static Direction single(String nodeName) {
        return new SingleDirection(nodeName);
    }

    public static Direction feedSubscribers(String feedName) {
        return new SubscribersDirection(SubscriptionType.FEED, feedName);
    }

    public static Direction postingSubscribers(UUID postingId) {
        return new SubscribersDirection(SubscriptionType.POSTING, postingId);
    }

    public static Direction postingCommentsSubscribers(UUID postingId) {
        return new SubscribersDirection(SubscriptionType.POSTING_COMMENTS, postingId);
    }

    public static Direction profileSubscribers() {
        return new SubscribersDirection(SubscriptionType.PROFILE);
    }

}
