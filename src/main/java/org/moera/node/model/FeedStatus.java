package org.moera.node.model;

public class FeedStatus {

    private int total;
    private int notViewed;
    private int notRead;

    public FeedStatus() {
    }

    public FeedStatus(int total, int notViewed, int notRead) {
        this.total = total;
        this.notViewed = notViewed;
        this.notRead = notRead;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
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
