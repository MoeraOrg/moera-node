package org.moera.node.model;

import java.util.List;

public class FeedSliceInfo {

    private long before;
    private long after;
    private List<StoryInfo> stories;
    private int total;
    private int totalInPast;
    private int totalInFuture;
    private int notViewed;
    private int notRead;

    public FeedSliceInfo() {
    }

    public long getBefore() {
        return before;
    }

    public void setBefore(long before) {
        this.before = before;
    }

    public long getAfter() {
        return after;
    }

    public void setAfter(long after) {
        this.after = after;
    }

    public List<StoryInfo> getStories() {
        return stories;
    }

    public void setStories(List<StoryInfo> stories) {
        this.stories = stories;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalInPast() {
        return totalInPast;
    }

    public void setTotalInPast(int totalInPast) {
        this.totalInPast = totalInPast;
    }

    public int getTotalInFuture() {
        return totalInFuture;
    }

    public void setTotalInFuture(int totalInFuture) {
        this.totalInFuture = totalInFuture;
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

    public void setStatus(FeedStatus status) {
        setTotal(status.getTotal());
        setNotViewed(status.getNotViewed());
        setNotRead(status.getNotRead());
    }

}
