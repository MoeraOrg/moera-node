package org.moera.node.model.notification;

import java.util.UUID;

public class FeedPostingAddedNotification extends SubscriberNotification {

    private String feedName;
    private String postingId;

    public FeedPostingAddedNotification() {
        super(NotificationType.FEED_POSTING_ADDED);
    }

    public FeedPostingAddedNotification(String feedName, UUID postingId) {
        super(NotificationType.FEED_POSTING_ADDED);
        this.feedName = feedName;
        this.postingId = postingId.toString();
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

}
