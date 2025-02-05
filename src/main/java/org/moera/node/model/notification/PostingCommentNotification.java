package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.moera.lib.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public abstract class PostingCommentNotification extends PostingSubscriberNotification {

    @Size(max = 36)
    private String commentId;

    @Size(max = 63)
    private String commentOwnerName;

    @Size(max = 96)
    private String commentOwnerFullName;

    @Size(max = 31)
    private String commentOwnerGender;

    @Valid
    private AvatarImage commentOwnerAvatar;

    public PostingCommentNotification(NotificationType type) {
        super(type);
    }

    public PostingCommentNotification(NotificationType type, UUID postingId, UUID commentId,
                                      String commentOwnerName, String commentOwnerFullName, String commentOwnerGender,
                                      AvatarImage commentOwnerAvatar) {
        super(type, postingId.toString());
        this.commentId = commentId.toString();
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
        this.commentOwnerGender = commentOwnerGender;
        this.commentOwnerAvatar = commentOwnerAvatar;
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

    public String getCommentOwnerGender() {
        return commentOwnerGender;
    }

    public void setCommentOwnerGender(String commentOwnerGender) {
        this.commentOwnerGender = commentOwnerGender;
    }

    public AvatarImage getCommentOwnerAvatar() {
        return commentOwnerAvatar;
    }

    public void setCommentOwnerAvatar(AvatarImage commentOwnerAvatar) {
        this.commentOwnerAvatar = commentOwnerAvatar;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("commentId", LogUtil.format(commentId)));
        parameters.add(Pair.of("commentOwnerName", LogUtil.format(commentOwnerName)));
    }

}
