package org.moera.node.notification.send;

import java.util.UUID;

import org.moera.node.data.SubscriptionType;

public class Directions {

    @Deprecated
    public static Direction single(String nodeName) {
        return new SingleDirection(nodeName);
    }

    public static Direction single(UUID nodeId, String nodeName) {
        return new SingleDirection(nodeId, nodeName);
    }

    @Deprecated
    public static Direction feedSubscribers(String feedName) {
        return new SubscribersDirection(SubscriptionType.FEED, feedName);
    }

    public static Direction feedSubscribers(UUID nodeId, String feedName) {
        return new SubscribersDirection(nodeId, SubscriptionType.FEED, feedName);
    }

    @Deprecated
    public static Direction postingSubscribers(UUID postingId) {
        return new SubscribersDirection(SubscriptionType.POSTING, postingId);
    }

    public static Direction postingSubscribers(UUID nodeId, UUID postingId) {
        return new SubscribersDirection(nodeId, SubscriptionType.POSTING, postingId);
    }

    @Deprecated
    public static Direction postingCommentsSubscribers(UUID postingId) {
        return new SubscribersDirection(SubscriptionType.POSTING_COMMENTS, postingId);
    }

    public static Direction postingCommentsSubscribers(UUID nodeId, UUID postingId) {
        return new SubscribersDirection(nodeId, SubscriptionType.POSTING_COMMENTS, postingId);
    }

    @Deprecated
    public static Direction profileSubscribers() {
        return new SubscribersDirection(SubscriptionType.PROFILE);
    }

    public static Direction profileSubscribers(UUID nodeId) {
        return new SubscribersDirection(nodeId, SubscriptionType.PROFILE);
    }

}
