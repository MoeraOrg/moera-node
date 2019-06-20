package org.moera.node.model;

import java.util.List;

public class TimelineSliceInfo {

    private long before;
    private long after;
    private List<PostingInfo> postings;

    public TimelineSliceInfo() {
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

    public List<PostingInfo> getPostings() {
        return postings;
    }

    public void setPostings(List<PostingInfo> postings) {
        this.postings = postings;
    }

}
