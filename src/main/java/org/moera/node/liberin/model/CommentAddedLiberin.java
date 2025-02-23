package org.moera.node.liberin.model;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.Comment;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfoUtil;
import org.moera.node.model.PostingInfoUtil;

public class CommentAddedLiberin extends Liberin {

    private Posting posting;
    private Comment comment;

    public CommentAddedLiberin(Posting posting, Comment comment) {
        this.posting = posting;
        this.comment = comment;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
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
        posting = entityManager.merge(posting);
        comment = entityManager.merge(comment);
        model.put("posting", PostingInfoUtil.build(posting, AccessCheckers.ADMIN));
        model.put("comment", CommentInfoUtil.build(comment, AccessCheckers.ADMIN));
    }

}
