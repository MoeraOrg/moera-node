package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public abstract class CommentReactionNotification extends ReactionNotification {

    @Size(max = 36)
    private String postingId;

    @Size(max = 36)
    private String commentId;

    protected CommentReactionNotification(NotificationType type) {
        super(type);
    }

    public CommentReactionNotification(NotificationType type, UUID postingId, UUID commentId, String ownerName,
                                       String ownerFullName, String ownerGender, AvatarImage ownerAvatar,
                                       boolean negative) {
        super(type, ownerName, ownerFullName, ownerGender, ownerAvatar, negative);
        this.postingId = postingId.toString();
        this.commentId = commentId.toString();
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

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
        parameters.add(Pair.of("commentId", LogUtil.format(commentId)));
    }

}
