package org.moera.node.liberin.model;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.Comment;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfoUtil;

public class CommentReactionsDeletedAllLiberin extends Liberin {

    private Comment comment;

    public CommentReactionsDeletedAllLiberin(Comment comment) {
        this.comment = comment;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    @Override
    protected void toModel(Map<String, Object> model, EntityManager entityManager) {
        super.toModel(model);
        comment = entityManager.merge(comment);
        model.put("comment", CommentInfoUtil.build(comment, AccessCheckers.ADMIN));
    }

}
