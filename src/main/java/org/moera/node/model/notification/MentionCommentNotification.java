package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import javax.validation.constraints.Size;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public abstract class MentionCommentNotification extends Notification {

    @Size(max = 36)
    private String postingId;

    @Size(max = 36)
    private String commentId;

    protected MentionCommentNotification(NotificationType type) {
        super(type);
    }

    public MentionCommentNotification(NotificationType type, UUID postingId, UUID commentId) {
        super(type);
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
