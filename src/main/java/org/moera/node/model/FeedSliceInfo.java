package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedSliceInfo {

    private long before;
    private long after;
    private List<StoryInfo> stories;
    private int totalInPast;
    private int totalInFuture;

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

}
