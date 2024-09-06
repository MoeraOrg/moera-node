package org.moera.node.data;

import java.util.Date;
import java.util.UUID;

public class SearchEngineClicks {

    private String nodeName;
    private UUID postingId;
    private UUID commentId;
    private UUID mediaId;
    private String heading;
    private long clicks;
    private Date lastClickedAt;

    public SearchEngineClicks() {
    }

    public SearchEngineClicks(String nodeName, UUID postingId, UUID commentId, UUID mediaId, String heading,
                              long clicks, Date lastClickedAt) {
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

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

    public UUID getCommentId() {
        return commentId;
    }

    public void setCommentId(UUID commentId) {
        this.commentId = commentId;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
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
