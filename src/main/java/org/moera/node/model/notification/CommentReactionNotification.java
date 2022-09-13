package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.moera.node.model.AvatarImage;
import org.springframework.data.util.Pair;

public abstract class CommentReactionNotification extends ReactionNotification {

    private String postingId;
    private String commentId;

    protected CommentReactionNotification(NotificationType type) {
        super(type);
    }

    public CommentReactionNotification(NotificationType type, UUID postingId, UUID commentId, String ownerName,
                                       String ownerFullName, AvatarImage ownerAvatar, boolean negative) {
        super(type, ownerName, ownerFullName, ownerAvatar, negative);
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
