package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public abstract class ReplyCommentNotification extends Notification {

    private String postingId;
    private String commentId;
    private String repliedToId;

    protected ReplyCommentNotification(NotificationType type) {
        super(type);
    }

    public ReplyCommentNotification(NotificationType type, UUID postingId, UUID commentId, UUID repliedToId) {
        super(type);
        this.postingId = postingId.toString();
        this.commentId = commentId.toString();
        this.repliedToId = repliedToId.toString();
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

    public String getRepliedToId() {
        return repliedToId;
    }

    public void setRepliedToId(String repliedToId) {
        this.repliedToId = repliedToId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
        parameters.add(Pair.of("commentId", LogUtil.format(commentId)));
        parameters.add(Pair.of("repliedToId", LogUtil.format(repliedToId)));
    }

}
