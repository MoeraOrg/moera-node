package org.moera.node.model.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriptionType;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriberEvent extends Event {

    private String id;
    private SubscriptionType subscriptionType;
    private String feedName;
    private String postingId;
    private String nodeName;
    private Long createdAt;

    public SubscriberEvent(EventType type) {
        super(type);
    }

    public SubscriberEvent(EventType type, Subscriber subscriber) {
        super(type);

        id = subscriber.getId().toString();
        subscriptionType = subscriber.getSubscriptionType();
        feedName = subscriber.getFeedName();
        if (subscriber.getEntry() != null) {
            postingId = subscriber.getEntry().getId().toString();
        }
        nodeName = subscriber.getRemoteNodeName();
        createdAt = Util.toEpochSecond(subscriber.getCreatedAt());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

}
