package org.moera.node.liberin.model;

import org.moera.node.data.Comment;
import org.moera.node.liberin.Liberin;

public class CommentDeletedLiberin extends Liberin {

    private Comment comment;

    public CommentDeletedLiberin(Comment comment) {
        this.comment = comment;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

}
