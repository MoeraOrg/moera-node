package org.moera.node.notification.send;

import java.util.UUID;

import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class Directions {

    public static Direction single(UUID nodeId, String nodeName) {
        return new SingleDirection(nodeId, nodeName);
    }

    public static Direction single(UUID nodeId, String nodeName, PrincipalFilter filter) {
        return new SingleDirection(nodeId, nodeName, filter);
    }

    public static Direction feedSubscribers(UUID nodeId, String feedName) {
        return new SubscribersDirection(nodeId, SubscriptionType.FEED, feedName);
    }

    public static Direction feedSubscribers(UUID nodeId, String feedName, PrincipalFilter filter) {
        return new SubscribersDirection(nodeId, SubscriptionType.FEED, feedName, filter);
    }

    public static Direction postingSubscribers(UUID nodeId, UUID postingId) {
        return new SubscribersDirection(nodeId, SubscriptionType.POSTING, postingId);
    }

    public static Direction postingSubscribers(UUID nodeId, UUID postingId, PrincipalFilter filter) {
        return new SubscribersDirection(nodeId, SubscriptionType.POSTING, postingId, filter);
    }

    public static Direction postingCommentsSubscribers(UUID nodeId, UUID postingId) {
        return new SubscribersDirection(nodeId, SubscriptionType.POSTING_COMMENTS, postingId);
    }

    public static Direction postingCommentsSubscribers(UUID nodeId, UUID postingId, PrincipalFilter filter) {
        return new SubscribersDirection(nodeId, SubscriptionType.POSTING_COMMENTS, postingId, filter);
    }

    public static Direction profileSubscribers(UUID nodeId) {
        return new SubscribersDirection(nodeId, SubscriptionType.PROFILE);
    }

    public static Direction profileSubscribers(UUID nodeId, PrincipalFilter filter) {
        return new SubscribersDirection(nodeId, SubscriptionType.PROFILE, filter);
    }

    public static Direction friends(UUID nodeId, UUID friendGroupId) {
        return new FriendGroupDirection(nodeId, friendGroupId);
    }

    public static Direction friends(UUID nodeId, UUID friendGroupId, PrincipalFilter filter) {
        return new FriendGroupDirection(nodeId, friendGroupId, filter);
    }

    public static Direction userListSubscribers(UUID nodeId, String listName) {
        return new SubscribersDirection(nodeId, SubscriptionType.USER_LIST, listName);
    }

    public static Direction userListSubscribers(UUID nodeId, String listName, PrincipalFilter filter) {
        return new SubscribersDirection(nodeId, SubscriptionType.USER_LIST, listName, filter);
    }

    public static Direction searchSubscribers(UUID nodeId) {
        return new SubscribersDirection(nodeId, SubscriptionType.SEARCH);
    }

    public static Direction searchSubscribers(UUID nodeId, PrincipalFilter filter) {
        return new SubscribersDirection(nodeId, SubscriptionType.SEARCH, filter);
    }

}
