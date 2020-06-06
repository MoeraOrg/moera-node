package org.moera.node.notification.send;

import org.moera.node.data.SubscriptionType;

public class Directions {

    public static Direction single(String nodeName) {
        return new SingleDirection(nodeName);
    }

    public static Direction feedSubscribers(String feedName) {
        return new SubscribersDirection(SubscriptionType.FEED, feedName);
    }

}
