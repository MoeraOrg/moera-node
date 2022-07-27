package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Comment;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfo;

public class CommentReactionTotalsUpdatedLiberin extends Liberin {

    private Comment comment;

    public CommentReactionTotalsUpdatedLiberin(Comment comment) {
        this.comment = comment;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("comment", new CommentInfo(comment, AccessCheckers.ADMIN));
    }

}
