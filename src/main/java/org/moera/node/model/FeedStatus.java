package org.moera.node.model;

public class FeedStatus {

    private int notViewed;
    private int notRead;

    public FeedStatus() {
    }

    public FeedStatus(int notViewed, int notRead) {
        this.notViewed = notViewed;
        this.notRead = notRead;
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
