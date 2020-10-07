package org.moera.node.model;

import java.util.List;

public class CommentsSliceInfo {

    private long before;
    private long after;
    private List<CommentInfo> comments;
    private int totalInPast;
    private int totalInFuture;

    public CommentsSliceInfo() {
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

    public List<CommentInfo> getComments() {
        return comments;
    }

    public void setComments(List<CommentInfo> comments) {
        this.comments = comments;
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
