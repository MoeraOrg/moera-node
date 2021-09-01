package org.moera.node.model;

public class FeedStatus {

    private int total;
    private int totalPinned;
    private int notViewed;
    private int notRead;

    public FeedStatus() {
    }

    public FeedStatus(int total, int totalPinned, int notViewed, int notRead) {
        this.total = total;
        this.totalPinned = totalPinned;
        this.notViewed = notViewed;
        this.notRead = notRead;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalPinned() {
        return totalPinned;
    }

    public void setTotalPinned(int totalPinned) {
        this.totalPinned = totalPinned;
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
