package org.moera.node.notification.send;

import java.util.UUID;

import org.moera.node.auth.principal.Principal;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.SubscriptionType;

class SubscribersDirection extends Direction {

    private SubscriptionType subscriptionType;
    private String feedName;
    private UUID postingId;

    SubscribersDirection(UUID nodeId, SubscriptionType subscriptionType) {
        this(nodeId, subscriptionType, null, null, PrincipalFilter.by(Principal.PUBLIC));
    }

    SubscribersDirection(UUID nodeId, SubscriptionType subscriptionType, PrincipalFilter filter) {
        this(nodeId, subscriptionType, null, null, filter);
    }

    SubscribersDirection(UUID nodeId, SubscriptionType subscriptionType, String feedName) {
        this(nodeId, subscriptionType, feedName, null, PrincipalFilter.by(Principal.PUBLIC));
    }

    SubscribersDirection(UUID nodeId, SubscriptionType subscriptionType, String feedName, PrincipalFilter filter) {
        this(nodeId, subscriptionType, feedName, null, filter);
    }

    SubscribersDirection(UUID nodeId, SubscriptionType subscriptionType, UUID postingId) {
        this(nodeId, subscriptionType, null, postingId, PrincipalFilter.by(Principal.PUBLIC));
    }

    SubscribersDirection(UUID nodeId, SubscriptionType subscriptionType, UUID postingId, PrincipalFilter filter) {
        this(nodeId, subscriptionType, null, postingId, filter);
    }

    private SubscribersDirection(UUID nodeId, SubscriptionType subscriptionType, String feedName, UUID postingId,
                                 PrincipalFilter filter) {
        super(nodeId, filter);
        this.subscriptionType = subscriptionType;
        this.feedName = feedName;
        this.postingId = postingId;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

}
