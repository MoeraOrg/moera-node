package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.data.Comment;
import org.moera.node.operations.MediaAttachmentsProvider;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentCreated {

    private CommentInfo comment;
    private Integer total;

    public CommentCreated() {
    }

    public CommentCreated(Comment comment, Integer total, MediaAttachmentsProvider mediaAttachmentsProvider,
                          AccessChecker accessChecker, List<BlockedOperation> blockedOperations) {
        if (comment != null) {
            this.comment = new CommentInfo(comment, mediaAttachmentsProvider, accessChecker);
            this.comment.putBlockedOperations(blockedOperations);
        }
        this.total = total;
    }

    public CommentInfo getComment() {
        return comment;
    }

    public void setComment(CommentInfo comment) {
        this.comment = comment;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

}
