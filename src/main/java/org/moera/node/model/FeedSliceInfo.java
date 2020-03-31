package org.moera.node.model;

import java.util.List;

public class FeedSliceInfo {

    private long before;
    private long after;
    private List<StoryInfo> stories;

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

}
