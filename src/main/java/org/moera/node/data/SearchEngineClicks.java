package org.moera.node.data;

import java.util.Date;

public class SearchEngineClicks {

    private String nodeName;
    private String postingId;
    private String commentId;
    private String mediaId;
    private String heading;
    private long clicks;
    private Date lastClickedAt;

    public SearchEngineClicks() {
    }

    public SearchEngineClicks(
        String nodeName, String postingId, String commentId, String mediaId, String heading, long clicks,
        Date lastClickedAt
    ) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.commentId = commentId;
        this.mediaId = mediaId;
        this.heading = heading;
        this.clicks = clicks;
        this.lastClickedAt = lastClickedAt;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public long getClicks() {
        return clicks;
    }

    public void setClicks(long clicks) {
        this.clicks = clicks;
    }

    public Date getLastClickedAt() {
        return lastClickedAt;
    }

    public void setLastClickedAt(Date lastClickedAt) {
        this.lastClickedAt = lastClickedAt;
    }

}
