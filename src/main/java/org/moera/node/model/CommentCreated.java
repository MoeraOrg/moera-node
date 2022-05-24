package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.data.Comment;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentCreated {

    private CommentInfo comment;
    private Integer total;

    public CommentCreated() {
    }

    public CommentCreated(Comment comment, Integer total, AccessChecker accessChecker) {
        this.comment = comment != null ? new CommentInfo(comment, accessChecker) : null;
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
