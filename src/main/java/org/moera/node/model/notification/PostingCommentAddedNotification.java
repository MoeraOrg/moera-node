package org.moera.node.model.notification;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public class PostingCommentAddedNotification extends PostingCommentNotification {

    private String postingHeading;
    private String commentHeading;
    private String commentRepliedTo;

    public PostingCommentAddedNotification() {
        super(NotificationType.POSTING_COMMENT_ADDED);
    }

    public PostingCommentAddedNotification(UUID postingId, String postingHeading, UUID commentId,
                                           String commentOwnerName, String commentOwnerFullName,
                                           AvatarImage commentOwnerAvatar, String commentHeading,
                                           UUID commentRepliedTo) {
        super(NotificationType.POSTING_COMMENT_ADDED, postingId, commentId, commentOwnerName, commentOwnerFullName,
                commentOwnerAvatar);
        this.postingHeading = postingHeading;
        this.commentHeading = commentHeading;
        this.commentRepliedTo = Objects.toString(commentRepliedTo, null);
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
