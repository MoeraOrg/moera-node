package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public abstract class CommentReactionNotification extends Notification {

    private String postingId;

    private String commentId;

    @Size(max = 63)
    private String ownerName;

    @Size(max = 96)
    private String ownerFullName;

    @Valid
    private AvatarImage ownerAvatar;

    private boolean negative;

    protected CommentReactionNotification(NotificationType type) {
        super(type);
    }

    public CommentReactionNotification(NotificationType type, UUID postingId, UUID commentId, String ownerName,
                                       String ownerFullName, AvatarImage ownerAvatar, boolean negative) {
        super(type);
        this.postingId = postingId.toString();
        this.commentId = commentId.toString();
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerAvatar = ownerAvatar;
        this.negative = negative;
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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public AvatarImage getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(AvatarImage ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
        parameters.add(Pair.of("commentId", LogUtil.format(commentId)));
        parameters.add(Pair.of("ownerName", LogUtil.format(ownerName)));
        parameters.add(Pair.of("negative", LogUtil.format(negative)));
    }

}
