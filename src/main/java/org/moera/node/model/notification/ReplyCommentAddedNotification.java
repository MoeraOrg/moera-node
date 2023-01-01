package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public class ReplyCommentAddedNotification extends ReplyCommentNotification {

    @Size(max = 63)
    private String postingOwnerName;

    @Size(max = 96)
    private String postingOwnerFullName;

    @Size(max = 31)
    private String postingOwnerGender;

    @Valid
    private AvatarImage postingOwnerAvatar;

    @Size(max = 255)
    private String postingHeading;

    @Size(max = 255)
    private String commentHeading;

    @Size(max = 255)
    private String repliedToHeading;

    public ReplyCommentAddedNotification() {
        super(NotificationType.REPLY_COMMENT_ADDED);
    }

    public ReplyCommentAddedNotification(String postingOwnerName, String postingOwnerFullName,
                                         String postingOwnerGender, AvatarImage postingOwnerAvatar, UUID postingId,
                                         UUID commentId, UUID repliedToId, String postingHeading,
                                         String commentOwnerName, String commentOwnerFullName,
                                         String commentOwnerGender, AvatarImage commentOwnerAvatar,
                                         String commentHeading, String repliedToHeading) {
        super(NotificationType.REPLY_COMMENT_ADDED, postingId, commentId, repliedToId, commentOwnerName,
                commentOwnerFullName, commentOwnerGender, commentOwnerAvatar);
        this.postingOwnerName = postingOwnerName;
        this.postingOwnerFullName = postingOwnerFullName;
        this.postingOwnerGender = postingOwnerGender;
        this.postingOwnerAvatar = postingOwnerAvatar;
        this.postingHeading = postingHeading;
        this.commentHeading = commentHeading;
        this.repliedToHeading = repliedToHeading;
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

    public String getPostingOwnerGender() {
        return postingOwnerGender;
    }

    public void setPostingOwnerGender(String postingOwnerGender) {
        this.postingOwnerGender = postingOwnerGender;
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
