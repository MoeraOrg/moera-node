package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;
import javax.validation.constraints.Size;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public class ReplyCommentDeletedNotification extends ReplyCommentNotification {

    @Size(max = 63)
    private String commentOwnerName;

    @Size(max = 96)
    private String commentOwnerFullName;

    public ReplyCommentDeletedNotification() {
        super(NotificationType.REPLY_COMMENT_DELETED);
    }

    public ReplyCommentDeletedNotification(UUID postingId, UUID commentId, UUID repliedToId, String commentOwnerName,
                                           String commentOwnerFullName) {
        super(NotificationType.REPLY_COMMENT_DELETED, postingId, commentId, repliedToId);
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
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

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("commentOwnerName", LogUtil.format(commentOwnerName)));
    }

}
