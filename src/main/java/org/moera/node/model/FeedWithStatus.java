package org.moera.node.model;

public class FeedWithStatus {

    private String feedName;
    private int notViewed;
    private int notRead;
    private Long notViewedMoment;
    private Long notReadMoment;

    public FeedWithStatus() {
    }

    public FeedWithStatus(String feedName, FeedStatus feedStatus) {
        this.feedName = feedName;
        notViewed = feedStatus.getNotViewed();
        notRead = feedStatus.getNotRead();
        notViewedMoment = feedStatus.getNotViewedMoment();
        notReadMoment = feedStatus.getNotReadMoment();
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

    public Long getNotViewedMoment() {
        return notViewedMoment;
    }

    public void setNotViewedMoment(Long notViewedMoment) {
        this.notViewedMoment = notViewedMoment;
    }

    public Long getNotReadMoment() {
        return notReadMoment;
    }

    public void setNotReadMoment(Long notReadMoment) {
        this.notReadMoment = notReadMoment;
    }

}
