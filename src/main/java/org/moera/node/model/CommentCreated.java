package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.data.BlockedOperation;
import org.moera.node.data.Comment;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentCreated {

    private CommentInfo comment;
    private Integer total;

    public CommentCreated() {
    }

    public CommentCreated(Comment comment, Integer total, AccessChecker accessChecker,
                          List<BlockedOperation> blockedOperations) {
        if (comment != null) {
            this.comment = new CommentInfo(comment, accessChecker);
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
