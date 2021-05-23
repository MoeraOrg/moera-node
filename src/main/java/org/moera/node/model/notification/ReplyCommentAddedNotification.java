package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public class ReplyCommentAddedNotification extends ReplyCommentNotification {

    private String postingHeading;

    private String commentHeading;

    private String repliedToHeading;

    public ReplyCommentAddedNotification() {
        super(NotificationType.REPLY_COMMENT_ADDED);
    }

    public ReplyCommentAddedNotification(UUID postingId, UUID commentId, UUID repliedToId, String postingHeading,
                                         String commentOwnerName, String commentOwnerFullName,
                                         AvatarImage commentOwnerAvatar, String commentHeading,
                                         String repliedToHeading) {
        super(NotificationType.REPLY_COMMENT_ADDED, postingId, commentId, repliedToId, commentOwnerName,
                commentOwnerFullName, commentOwnerAvatar);
        this.postingHeading = postingHeading;
        this.commentHeading = commentHeading;
        this.repliedToHeading = repliedToHeading;
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

    public String getRepliedToHeading() {
        return repliedToHeading;
    }

    public void setRepliedToHeading(String repliedToHeading) {
        this.repliedToHeading = repliedToHeading;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("commentHeading", LogUtil.format(commentHeading)));
    }

}
