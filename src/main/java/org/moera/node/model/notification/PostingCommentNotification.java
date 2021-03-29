package org.moera.node.model.notification;

import java.util.UUID;
import javax.validation.constraints.Size;

public abstract class PostingCommentNotification extends PostingSubscriberNotification {

    private String postingId;

    private String commentId;

    @Size(max = 63)
    private String commentOwnerName;

    @Size(max = 96)
    private String commentOwnerFullName;

    public PostingCommentNotification(NotificationType type) {
        super(type);
    }

    public PostingCommentNotification(NotificationType type, UUID postingId, UUID commentId,
                                      String commentOwnerName, String commentOwnerFullName) {
        super(type, postingId.toString());
        this.commentId = commentId.toString();
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getCommentOwnerName() {
        return commentOwnerName;
    }

    public void setCommentOwnerName(String commentOwnerName) {
        this.commentOwnerName = commentOwnerName;
    }

    public String getCommentOwnerFullName() {
        return commentOwnerFullName;
    }

    public void setCommentOwnerFullName(String commentOwnerFullName) {
        this.commentOwnerFullName = commentOwnerFullName;
    }

}
