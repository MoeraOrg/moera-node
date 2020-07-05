package org.moera.node.model;

import java.util.List;

public class FeedSliceInfoR {

    private long before;
    private long after;
    private List<StoryInfoR> stories;

    public FeedSliceInfoR() {
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

    public List<StoryInfoR> getStories() {
        return stories;
    }

    public void setStories(List<StoryInfoR> stories) {
        this.stories = stories;
    }

}
