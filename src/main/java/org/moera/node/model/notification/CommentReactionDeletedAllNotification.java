package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public class CommentReactionDeletedAllNotification extends Notification {

    private String postingId;
    private String commentId;

    public CommentReactionDeletedAllNotification() {
        super(NotificationType.COMMENT_REACTION_DELETED_ALL);
    }

    public CommentReactionDeletedAllNotification(UUID postingId, UUID commentId) {
        super(NotificationType.COMMENT_REACTION_DELETED_ALL);
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
