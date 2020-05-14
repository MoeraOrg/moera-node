package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Subscriber;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionInfo {

    private String id;
    private String type;
    private String feedName;
    private String postingId;
    private String nodeName;
    private Long createdAt;

    public SubscriptionInfo() {
    }

    public SubscriptionInfo(Subscriber subscriber) {
        id = subscriber.getId().toString();
        type = subscriber.getSubscriptionType().getValue();
        feedName = subscriber.getFeedName();
        postingId = subscriber.getEntry() != null ? subscriber.getEntry().getId().toString() : null;
        nodeName = subscriber.getRemoteNodeName();
        createdAt = Util.toEpochSecond(subscriber.getCreatedAt());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
