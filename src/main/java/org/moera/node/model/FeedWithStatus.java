package org.moera.node.model;

public class FeedWithStatus {

    private String feedName;
    private int notViewed;
    private int notRead;

    public FeedWithStatus() {
    }

    public FeedWithStatus(String feedName, FeedStatus feedStatus) {
        this.feedName = feedName;
        notViewed = feedStatus.getNotViewed();
        notRead = feedStatus.getNotRead();
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public int getNotViewed() {
        return notViewed;
    }

    public void setNotViewed(int notViewed) {
        this.notViewed = notViewed;
    }

    public int getNotRead() {
        return notRead;
    }

    public void setNotRead(int notRead) {
        this.notRead = notRead;
    }

}
