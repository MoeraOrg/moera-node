package org.moera.node.liberin.model;

import org.moera.lib.node.types.AvatarImage;
import org.moera.node.data.Comment;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;

public class SheriffOrderReceivedLiberin extends Liberin {

    private boolean deleted;
    private String feedName;
    private Posting posting;
    private Comment comment;
    private String sheriffName;
    private AvatarImage sheriffAvatar;
    private String orderId;

    public SheriffOrderReceivedLiberin(boolean deleted, String feedName, Posting posting, Comment comment,
                                       String sheriffName, AvatarImage sheriffAvatar, String orderId) {
        this.deleted = deleted;
        this.feedName = feedName;
        this.posting = posting;
        this.comment = comment;
        this.sheriffName = sheriffName;
        this.sheriffAvatar = sheriffAvatar;
        this.orderId = orderId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public String getSheriffName() {
        return sheriffName;
    }

    public void setSheriffName(String sheriffName) {
        this.sheriffName = sheriffName;
    }

    public AvatarImage getSheriffAvatar() {
        return sheriffAvatar;
    }

    public void setSheriffAvatar(AvatarImage sheriffAvatar) {
        this.sheriffAvatar = sheriffAvatar;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

}
