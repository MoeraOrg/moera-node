package org.moera.node.model.notification;

public class FeedPostingAddedNotification extends SubscriberNotification {

    private String feedName;
    private String postingId;

    public FeedPostingAddedNotification(String feedName, String postingId) {
        super(NotificationType.FEED_POSTING_ADDED);
        this.feedName = feedName;
        this.postingId = postingId;
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
