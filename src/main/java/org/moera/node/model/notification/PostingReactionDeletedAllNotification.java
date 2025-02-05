package org.moera.node.model.notification;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.validation.constraints.Size;

import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class PostingReactionDeletedAllNotification extends Notification {

    @Size(max = 36)
    private String parentPostingId;

    @Size(max = 36)
    private String parentCommentId;

    @Size(max = 36)
    private String parentMediaId;

    @Size(max = 36)
    private String postingId;

    public PostingReactionDeletedAllNotification() {
        super(NotificationType.POSTING_REACTION_DELETED_ALL);
    }

    public PostingReactionDeletedAllNotification(UUID parentPostingId, UUID parentCommentId, UUID parentMediaId,
                                                 UUID postingId) {
        super(NotificationType.POSTING_REACTION_DELETED_ALL);
        this.parentPostingId = Objects.toString(parentPostingId, null);
        this.parentCommentId = Objects.toString(parentCommentId, null);
        this.parentMediaId = Objects.toString(parentMediaId, null);
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
