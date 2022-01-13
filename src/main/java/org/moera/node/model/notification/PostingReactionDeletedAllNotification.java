package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public class PostingReactionDeletedAllNotification extends Notification {

    private String parentPostingId;

    private String parentCommentId;

    private String parentMediaId;

    private String postingId;

    protected PostingReactionDeletedAllNotification() {
        super(NotificationType.POSTING_REACTION_DELETED_ALL);
    }

    public PostingReactionDeletedAllNotification(UUID parentPostingId, UUID parentCommentId, UUID parentMediaId,
                                                 UUID postingId) {
        super(NotificationType.POSTING_REACTION_DELETED_ALL);
        this.parentPostingId = parentPostingId != null ? parentPostingId.toString() : null;
        this.parentCommentId = parentCommentId != null ? parentCommentId.toString() : null;
        this.parentMediaId = parentMediaId != null ? parentMediaId.toString() : null;
        this.postingId = postingId.toString();
    }

    public String getParentPostingId() {
        return parentPostingId;
    }

    public void setParentPostingId(String parentPostingId) {
        this.parentPostingId = parentPostingId;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getParentMediaId() {
        return parentMediaId;
    }

    public void setParentMediaId(String parentMediaId) {
        this.parentMediaId = parentMediaId;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("parentPostingId", LogUtil.format(parentPostingId)));
        parameters.add(Pair.of("parentCommentId", LogUtil.format(parentCommentId)));
        parameters.add(Pair.of("parentMediaId", LogUtil.format(parentMediaId)));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
    }

}