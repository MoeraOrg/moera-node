package org.moera.node.model.notification;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public class PostingCommentAddedNotification extends PostingCommentNotification {

    private String postingOwnerName;
    private String postingOwnerFullName;
    private AvatarImage postingOwnerAvatar;
    private String postingHeading;
    private String commentHeading;
    private String commentRepliedTo;

    public PostingCommentAddedNotification() {
        super(NotificationType.POSTING_COMMENT_ADDED);
    }

    public PostingCommentAddedNotification(String postingOwnerName, String postingOwnerFullName,
                                           AvatarImage postingOwnerAvatar, UUID postingId, String postingHeading,
                                           UUID commentId, String commentOwnerName, String commentOwnerFullName,
                                           AvatarImage commentOwnerAvatar, String commentHeading,
                                           UUID commentRepliedTo) {
        super(NotificationType.POSTING_COMMENT_ADDED, postingId, commentId, commentOwnerName, commentOwnerFullName,
                commentOwnerAvatar);
        this.postingOwnerName = postingOwnerName;
        this.postingOwnerFullName = postingOwnerFullName;
        this.postingOwnerAvatar = postingOwnerAvatar;
        this.postingHeading = postingHeading;
        this.commentHeading = commentHeading;
        this.commentRepliedTo = Objects.toString(commentRepliedTo, null);
    }

    public String getPostingOwnerName() {
        return postingOwnerName;
    }

    public void setPostingOwnerName(String postingOwnerName) {
        this.postingOwnerName = postingOwnerName;
    }

    public String getPostingOwnerFullName() {
        return postingOwnerFullName;
    }

    public void setPostingOwnerFullName(String postingOwnerFullName) {
        this.postingOwnerFullName = postingOwnerFullName;
    }

    public AvatarImage getPostingOwnerAvatar() {
        return postingOwnerAvatar;
    }

    public void setPostingOwnerAvatar(AvatarImage postingOwnerAvatar) {
        this.postingOwnerAvatar = postingOwnerAvatar;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
    }

    public String getCommentRepliedTo() {
        return commentRepliedTo;
    }

    public void setCommentRepliedTo(String commentRepliedTo) {
        this.commentRepliedTo = commentRepliedTo;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("commentHeading", LogUtil.format(commentHeading)));
    }

}
