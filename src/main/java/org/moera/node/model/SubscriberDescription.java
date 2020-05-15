package org.moera.node.model;

import java.util.UUID;

public class SubscriberDescription {

    private String type;
    private String feedName;
    private UUID postingId;

    public SubscriberDescription() {
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

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

}
