package org.moera.node.notification.send;

import java.util.UUID;

import org.moera.node.data.SubscriptionType;

public class Directions {

    public static Direction single(UUID nodeId, String nodeName) {
        return new SingleDirection(nodeId, nodeName);
    }

    public static Direction feedSubscribers(UUID nodeId, String feedName) {
        return new SubscribersDirection(nodeId, SubscriptionType.FEED, feedName);
    }

    public static Direction postingSubscribers(UUID nodeId, UUID postingId) {
        return new SubscribersDirection(nodeId, SubscriptionType.POSTING, postingId);
    }

    public static Direction postingCommentsSubscribers(UUID nodeId, UUID postingId) {
        return new SubscribersDirection(nodeId, SubscriptionType.POSTING_COMMENTS, postingId);
    }

    public static Direction profileSubscribers(UUID nodeId) {
        return new SubscribersDirection(nodeId, SubscriptionType.PROFILE);
    }

}
